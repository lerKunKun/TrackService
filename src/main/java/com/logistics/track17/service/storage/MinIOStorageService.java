package com.logistics.track17.service.storage;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/**
 * MinIO对象存储实现
 * 生产环境使用
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinIOStorageService implements StorageService {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private MinioClient minioClient;

    @PostConstruct
    public void init() throws Exception {
        // 初始化MinIO客户端
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // 确保bucket存在
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build());

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Created MinIO bucket: {}", bucketName);
        }
    }

    @Override
    public String upload(InputStream file, String fileName, String contentType) throws Exception {
        // 生成对象路径: theme-archives/shrine-pro-v3.0.0.zip
        String objectName = "theme-archives/" + fileName;

        // 上传到MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file, -1, 10485760) // 10MB part size
                        .contentType(contentType)
                        .build());

        log.info("Uploaded to MinIO: {}", objectName);
        return objectName;
    }

    @Override
    public Path download(String storagePath) throws Exception {
        // 下载到临时文件
        Path tempFile = Files.createTempFile("theme-", ".zip");

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(storagePath)
                        .build())) {
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Downloaded from MinIO: {} -> {}", storagePath, tempFile);
        return tempFile;
    }

    @Override
    public void delete(String storagePath) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(storagePath)
                        .build());

        log.info("Deleted from MinIO: {}", storagePath);
    }

    @Override
    public String getPresignedUrl(String storagePath, int expireSeconds) throws Exception {
        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(storagePath)
                        .expiry(expireSeconds, TimeUnit.SECONDS)
                        .build());

        return url;
    }

    @Override
    public boolean exists(String storagePath) throws Exception {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storagePath)
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
