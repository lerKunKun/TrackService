package com.logistics.track17.service;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    /**
     * 确保 bucket 存在，若不存在则创建并设置公开读策略
     */
    public void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                // 设置公开读策略（用于图片/视频直接访问）
                String policy = "{"
                        + "\"Version\":\"2012-10-17\","
                        + "\"Statement\":[{"
                        + "\"Effect\":\"Allow\","
                        + "\"Principal\":{\"AWS\":[\"*\"]},"
                        + "\"Action\":[\"s3:GetObject\"],"
                        + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]"
                        + "}]}";
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(policy)
                        .build());
                log.info("MinIO bucket '{}' created with public-read policy", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket '{}' exists", bucket, e);
            throw new RuntimeException("MinIO bucket init failed: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件，返回 objectName
     */
    public String uploadFile(String bucket, String objectName, MultipartFile file) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("File uploaded: bucket={}, object={}", bucket, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload file: {}", objectName, e);
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * 从 InputStream 上传（用于 URL 下载场景）
     */
    public String uploadStream(String bucket, String objectName, InputStream inputStream,
            long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            log.info("Stream uploaded: bucket={}, object={}", bucket, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload stream: {}", objectName, e);
            throw new RuntimeException("Stream upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String bucket, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            log.info("File deleted: bucket={}, object={}", bucket, objectName);
        } catch (Exception e) {
            log.error("Failed to delete file: {}", objectName, e);
            throw new RuntimeException("File delete failed: " + e.getMessage(), e);
        }
    }

    /**
     * 列出指定前缀下的所有文件
     */
    public List<MinioFileInfo> listFiles(String bucket, String prefix) {
        List<MinioFileInfo> result = new ArrayList<>();
        try {
            Iterable<Result<Item>> items = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix(prefix)
                            .recursive(true)
                            .build());
            for (Result<Item> itemResult : items) {
                Item item = itemResult.get();
                if (!item.isDir()) {
                    String objectName = item.objectName();
                    String fileName = objectName.contains("/")
                            ? objectName.substring(objectName.lastIndexOf('/') + 1)
                            : objectName;
                    // 提取 tag（路径第二段）
                    String[] parts = objectName.split("/");
                    String tag = parts.length >= 2 ? parts[1] : "other";
                    // 判断媒体类型
                    String mediaType = detectMediaType(fileName);
                    // 生成直接访问 URL（公开 bucket）
                    String url = getDirectUrl(bucket, objectName);
                    result.add(new MinioFileInfo(objectName, fileName, tag, mediaType, item.size(), url));
                }
            }
        } catch (Exception e) {
            log.error("Failed to list files with prefix: {}", prefix, e);
            throw new RuntimeException("File list failed: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 生成预签 URL（适用于私有 bucket，时效 7 天）
     */
    public String getPresignedUrl(String bucket, String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to get presigned URL for: {}", objectName, e);
            throw new RuntimeException("Presigned URL generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * 对于公开 bucket，生成直接访问 URL
     */
    public String getDirectUrl(String bucket, String objectName) {
        try {
            // 获取 MinioClient 的 endpoint
            String endpoint = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
            // 截取 endpoint 部分（去掉签名参数）作为公开 URL
            return endpoint.split("\\?")[0];
        } catch (Exception e) {
            log.warn("Failed to get direct URL for: {}", objectName);
            return "";
        }
    }

    private String detectMediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov")
                || lower.endsWith(".avi") || lower.endsWith(".mkv")) {
            return "video";
        }
        return "image";
    }

    /**
     * 文件信息 DTO
     */
    public static class MinioFileInfo {
        public String objectName;
        public String fileName;
        public String tag;
        public String mediaType;
        public long size;
        public String url;

        public MinioFileInfo(String objectName, String fileName, String tag,
                String mediaType, long size, String url) {
            this.objectName = objectName;
            this.fileName = fileName;
            this.tag = tag;
            this.mediaType = mediaType;
            this.size = size;
            this.url = url;
        }
    }
}
