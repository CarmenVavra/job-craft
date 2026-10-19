package com.jobcraft.config;

import com.jobcraft.service.storage.LocalStorageService;
import com.jobcraft.service.storage.S3StorageService;
import com.jobcraft.service.storage.StorageService;
import java.net.URI;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Wählt die Dateiablage anhand von {@code jobcraft.storage.type} ({@code local} oder {@code s3}).
 */
@Configuration
public class StorageConfig {

    @Bean
    public StorageService storageService(JobCraftProperties properties) {
        JobCraftProperties.Storage storage = properties.storage();
        if ("s3".equalsIgnoreCase(storage.type())) {
            JobCraftProperties.S3 s3 = storage.s3();
            S3Client client = S3Client.builder()
                    .endpointOverride(URI.create(s3.endpoint()))
                    .region(Region.of(s3.region()))
                    .forcePathStyle(s3.pathStyleAccess())
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(s3.accessKey(), s3.secretKey())))
                    .build();
            return new S3StorageService(client, s3.bucket());
        }
        return new LocalStorageService(Path.of(storage.localPath()));
    }
}
