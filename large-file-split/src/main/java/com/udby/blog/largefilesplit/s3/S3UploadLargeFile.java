package com.udby.blog.largefilesplit.s3;

import com.udby.blog.largefilesplit.LargeFileSplitter;
import com.udby.blog.largefilesplit.SimpleByteBufferBodyPublisher;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static com.udby.blog.largefilesplit.LargeFileSplitter.SIZE_64M;

public class S3UploadLargeFile {
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    private final AmazonS3Facade amazonS3Facade;
    private final int blockSize;
    private final Semaphore maxParallelism;

    private final MessageDigestHelper messageDigestHelper = MessageDigestHelper.MD5;

    private final AtomicInteger completedParts = new AtomicInteger();
    private final AtomicInteger startedParts = new AtomicInteger();
    private final AtomicLong completedBytes = new AtomicLong();
    private final AtomicBoolean changed = new AtomicBoolean(true);

    public S3UploadLargeFile() {
        this((int) LargeFileSplitter.SIZE_32M, 64);
    }

    public S3UploadLargeFile(int blockSize, int maxParallelism) {
        this(AmazonS3Facade.from(DefaultCredentialsProvider.create(), resolveRegion()), blockSize, maxParallelism);
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

        final var main = new S3UploadLargeFile((int) SIZE_64M, 48);

        try (final var x = Executors.newSingleThreadScheduledExecutor()) {
            x.scheduleAtFixedRate(main::progressLog, 2_000L, 125L, TimeUnit.MILLISECONDS);
            main.uploadToS3(fileToUpload, args[1]);
        } finally {
            main.changed.set(true);
            main.progressLog();
        }

    }

    public void uploadToS3(Path fileToUpload, String s3BucketPath) {
        if (!s3BucketPath.contains("/")) {
            System.out.printf("S3 bucket/path must contain initial '/' to separate S3 bucket and path: %s%n", s3BucketPath);
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

        final var t0 = System.nanoTime();

        final var uploadId = amazonS3Facade.prepareMultipartUpload(bucket, destination, mimetype);
        System.out.printf("Prepared multipart upload: %s%n", uploadId);

        final var largeFileSplitter = LargeFileSplitter.fromFile(fileToUpload, blockSize);
        final var completedParts = new ArrayList<UploadedPart>((int) (size / blockSize));
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

            if (largeFileSplitter.exception() == null) {
                amazonS3Facade.completeMultipartUpload(bucket, destination, uploadId, completedParts);
                System.out.println("Completed multipart upload");
                failed = false;
            } else {
                System.out.printf("Spilt failed: %s%n", largeFileSplitter.exception());
            }

            final var timingSeconds = 1e-9 * (System.nanoTime() - t0);
            System.out.printf("Timing: %.3fs %s/s%n", timingSeconds, format((long) ((size * 1000L) / (timingSeconds * 1000.0))));
        } finally {
            if (failed) {
                amazonS3Facade.abortMultipartUpload(bucket, destination, uploadId);
                System.out.println("Aborted multipart upload");
            }
        }
    }

    private UploadedPart uploadPart(HttpClient httpClient, String bucket, String key, String uploadId, int partNumber, ByteBuffer byteBuffer) throws IOException {
        try {
            maxParallelism.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        startedParts.incrementAndGet();
        changed.set(true);
        try {
            final var contentMd5 = md5(byteBuffer);
            final var presignedPutUri = amazonS3Facade.presignedUploadPartRequest(bucket, key, uploadId, partNumber, contentMd5, Duration.ofMinutes(10L));

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
            changed.set(true);

            return new UploadedPart(partNumber, eTag);
        } finally {
            maxParallelism.release();
        }
    }

    private void progressLog() {
        if (changed.compareAndSet(true, false)) {
            final var partsStarted = this.startedParts.get();
            final var partsCompleted = this.completedParts.get();
            final var bytesUploaded = this.completedBytes.get();
            System.out.printf("Parts started: %d completed: %d bytes uploaded: %s%n", partsStarted, partsCompleted, format(bytesUploaded));
        }
    }

    private static final String[] KIBS = {"", " kiB", " MiB", " GiB", " TiB"};

    private static String format(long value) {
        if (value == 0) {
            return "0 (zero)";
        }
        int kibs = 0;
        double tmp = value;
        while (tmp > 1024) {
            kibs++;
            tmp = tmp / 1024.0;
        }
        if (kibs > 4) {
            return Long.toString(value);
        }
        return "%.3f%s".formatted(tmp, KIBS[kibs]);
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
        try (final var md5Holder = messageDigestHelper.lease()) {
            final var messageDigest = md5Holder.messageDigest();
            messageDigest.update(byteBuffer);
            return BASE64_ENCODER.encodeToString(messageDigest.digest());
        } finally {
            byteBuffer.rewind();
        }
    }

    record UploadedPart(int partNumber, String eTag) implements AmazonS3Facade.UploadedPart {
    }
}
