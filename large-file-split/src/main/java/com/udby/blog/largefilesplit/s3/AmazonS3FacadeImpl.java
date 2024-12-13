package com.udby.blog.largefilesplit.s3;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.Collection;

public class AmazonS3FacadeImpl implements AmazonS3Facade {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public AmazonS3FacadeImpl(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public boolean checkBucketExists(String bucket) {
        try {
            s3Client.headBucket(requet -> requet.bucket(bucket));
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    @Override
    public String prepareMultipartUpload(String bucket, String key, String contentType) {
        final var result = s3Client.createMultipartUpload(multipartRequest ->
                multipartRequest.bucket(bucket)
                        .key(key)
                        .contentType(contentType));
        return result.uploadId();
    }

    @Override
    public void abortMultipartUpload(String bucket, String key, String uploadId) {
        try {
            s3Client.abortMultipartUpload(abortRequest ->
                    abortRequest.bucket(bucket)
                            .key(key)
                            .uploadId(uploadId));
        } catch (NoSuchUploadException ignored) {
            // Don't mind aborting non-existent uploads
        }
    }

    @Override
    public String presignedUploadPartRequest(String bucket, String key, String uploadId, int partNumber, String contentMd5, Duration signatureDuration) {
        final var presigned = s3Presigner.presignUploadPart(presignRequest ->
                presignRequest.uploadPartRequest(uploadPartRequest ->
                                uploadPartRequest.bucket(bucket)
                                        .contentMD5(contentMd5)
                                        .key(key)
                                        .partNumber(partNumber)
                                        .uploadId(uploadId))
                        .signatureDuration(signatureDuration));

        return presigned.url().toString();
    }

    @Override
    public void completeMultipartUpload(String bucket, String key, String uploadId, Collection<CompletedPart> parts) {
        s3Client.completeMultipartUpload(request ->
                request.bucket(bucket)
                        .key(key)
                        .multipartUpload(mp ->
                                mp.parts(parts))
                        .uploadId(uploadId));
    }
}
