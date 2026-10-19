package com.jobcraft.service.storage;

import com.jobcraft.exception.ExternalServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Speichert Dateien in einem S3-kompatiblen Object Storage (AWS S3, MinIO).
 */
public class S3StorageService implements StorageService {

    private final S3Client client;
    private final String bucket;

    public S3StorageService(S3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        try {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromBytes(content));
        } catch (SdkException e) {
            throw new ExternalServiceException("Die Datei konnte nicht im Object Storage gespeichert werden.", e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
        } catch (SdkException e) {
            throw new ExternalServiceException("Die Datei konnte nicht aus dem Object Storage gelesen werden.", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            throw new ExternalServiceException("Die Datei konnte nicht aus dem Object Storage gelöscht werden.", e);
        }
    }
}
