package com.udby.blog.largefilesplit.s3;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.Collection;

public interface AmazonS3Facade {
    static AmazonS3Facade from(AwsCredentialsProvider credentialsProvider, Region region) {
        final var s3Client = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();
        final var s3Presigner = S3Presigner.builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .s3Client(s3Client)
                .build();
        return new AmazonS3FacadeImpl(s3Client, s3Presigner);
    }

    boolean checkBucketExists(String bucket);

    String prepareMultipartUpload(String bucket, String key, String contentType);

    void abortMultipartUpload(String bucket, String key, String uploadId);

    String presignedUploadPartRequest(String bucket, String key, String uploadId, int partNumber, String contentMd5, final Duration signatureDuration);

    void completeMultipartUpload(String bucket, String key, String uploadId, Collection<CompletedPart> parts);
}
