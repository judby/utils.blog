package com.udby.blog.largefilesplit.s3;

import com.udby.blog.largefilesplit.LargeFileSplitter;
import com.udby.blog.largefilesplit.SimpleByteBufferBodyPublisher;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class S3UploadLargeFile {
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
    private static final MessageDigest MD5;

    static {
        try {
            MD5 = MessageDigest.getInstance("MD5");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private final Queue<MessageDigest> cachedMessageDigests = new ConcurrentLinkedQueue<>();
    private final AmazonS3Facade amazonS3Facade;
    private final int blockSize;
    private final AtomicInteger completedParts = new AtomicInteger();
    private final AtomicLong completedBytes = new AtomicLong();
    private final Semaphore maxParallelism;

    public S3UploadLargeFile() {
        this(AmazonS3Facade.from(DefaultCredentialsProvider.create(), resolveRegion()), (int) LargeFileSplitter.SIZE_32M, 32);
    }

    public S3UploadLargeFile(AmazonS3Facade amazonS3Facade, int blockSize, int maxParallelism) {
        this.amazonS3Facade = amazonS3Facade;
        this.blockSize = blockSize;
        this.maxParallelism = new Semaphore(maxParallelism);
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Arguments required:");
            System.out.println("1. name/path of file to upload");
            System.out.println("2. name of S3 bucket/path");
            System.exit(1);
        }
        final var fileToUpload = Path.of(args[0])
                .normalize()
                .toAbsolutePath();

        final var main = new S3UploadLargeFile();

        main.uploadToS3(fileToUpload, args[1]);
    }

    public void uploadToS3(Path fileToUpload, String s3BucketPath) {
        if (!s3BucketPath.contains("/")) {
            System.out.printf("S3 bucket/path must consist of bucket and path: %s%n", s3BucketPath);
            return;
        }

        final var pos = s3BucketPath.indexOf('/');
        final var bucket = s3BucketPath.substring(0, pos);
        final var path = s3BucketPath.substring(pos + 1);

        uploadToS3(fileToUpload, bucket, path);
    }

    public void uploadToS3(Path fileToUpload, String bucket, String s3Path) {
        if (!Files.isReadable(fileToUpload)) {
            System.out.printf("File is not readable: %s%n", fileToUpload);
            return;
        }

        final long size;
        try {
            size = Files.size(fileToUpload);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (!amazonS3Facade.checkBucketExists(bucket)) {
            System.out.printf("Bucket %s does not exist%n", bucket);
            return;
        }

        final var destination = Path.of(s3Path)
                .resolve(fileToUpload.getFileName())
                .toString();
        final var mimetype = Mimetype.getInstance()
                .getMimetype(fileToUpload);
        System.out.printf("Uploading %s (%.3f MiB %s)%n...to S3 bucket '%s'%n...as '%s'%n", fileToUpload, (1.0 * size / LargeFileSplitter.ONE_M) + 0.0005, mimetype, bucket, destination);

        final var uploadId = amazonS3Facade.prepareMultipartUpload(bucket, destination, mimetype);
        System.out.printf("Prepared multipart upload: %s%n", uploadId);

        final var largeFileSplitter = LargeFileSplitter.fromFile(fileToUpload, blockSize);
        final var completedParts = new ArrayList<CompletedPart>((int) (size / blockSize));
        final var lock = new ReentrantLock();
        boolean failed = true;
        try (final var httpClient = HttpClient.newHttpClient()) {
            largeFileSplitter.processInVirtualThreads(((partNumber, byteBuffer) -> {
                final var completedPart = uploadPart(httpClient, bucket, destination, uploadId, partNumber, byteBuffer);
                lock.lock();
                try {
                    completedParts.add(completedPart);
                } finally {
                    lock.unlock();
                }
            }));

            // They must be sorted by partNumber...
            completedParts.sort(Comparator.comparing(CompletedPart::partNumber));
            amazonS3Facade.completeMultipartUpload(bucket, destination, uploadId, completedParts);
            failed = false;
            System.out.println("Completed multipart upload");
        } finally {
            if (failed) {
                amazonS3Facade.abortMultipartUpload(bucket, destination, uploadId);
                System.out.println("Aborted multipart upload");
            }
        }
    }

    private CompletedPart uploadPart(HttpClient httpClient, String bucket, String key, String uploadId, int partNumber, ByteBuffer byteBuffer) throws IOException {
        try {
            maxParallelism.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        try {
            final var contentMd5 = md5(byteBuffer);
            final var presignedPutUri = amazonS3Facade.presignedUploadPartRequest(bucket, key, uploadId, partNumber, contentMd5, Duration.ofHours(1L));

            final var byteBufferBodyPublisher = new SimpleByteBufferBodyPublisher(byteBuffer);

            final var uri = URI.create(presignedPutUri);
            HttpRequest httpRequestUpload = HttpRequest.newBuilder()
                    .header("Content-MD5", contentMd5)
                    .uri(uri)
                    .PUT(byteBufferBodyPublisher)
                    .build();

            final HttpResponse<String> response;
            try {
                response = httpClient.send(httpRequestUpload, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }

            if (response.statusCode() != 200) {
                throw new IOException("HTTP Status: %d%n%s".formatted(response.statusCode(), response.body()));
            }

            final var eTag = response.headers()
                    .firstValue("ETag")
                    .orElseThrow();

            completedParts.incrementAndGet();
            completedBytes.addAndGet(byteBuffer.capacity());

            return CompletedPart.builder()
                    .eTag(eTag)
                    .partNumber(partNumber)
                    .build();
        } finally {
            maxParallelism.release();
        }
    }

    private static Region resolveRegion() {
        var region = regionFrom(System.getProperty("aws.region"));
        if (region != null) {
            region = regionFrom(System.getenv("AWS_REGION"));
        } else {
            region = regionFrom("eu-north-1");
        }
        System.out.printf("Region: %s%n", region);
        return region;
    }

    private static Region regionFrom(String s) {
        return s == null ? null : Region.of(s);
    }

    private String md5(ByteBuffer byteBuffer) {
        try (final var md5Holder = new MD5Holder()) {
            final var messageDigest = md5Holder.md5;
            messageDigest.update(byteBuffer);
            return BASE64_ENCODER.encodeToString(messageDigest.digest());
        } finally {
            byteBuffer.rewind();
        }
    }

    private class MD5Holder implements AutoCloseable {
        final MessageDigest md5;

        MD5Holder() {
            final var md5 = cachedMessageDigests.poll();
            try {
                this.md5 = md5 == null ? (MessageDigest) MD5.clone() : md5;
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void close() {
            this.md5.reset();
            cachedMessageDigests.offer(this.md5);
        }
    }
}
