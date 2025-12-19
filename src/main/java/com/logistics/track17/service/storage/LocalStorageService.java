package com.logistics.track17.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件存储实现
 * 开发环境使用
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${storage.local.base-path:./storage/theme-archives}")
    private String basePath;

    @Override
    public String upload(InputStream file, String fileName, String contentType) throws Exception {
        // 本地路径: ./storage/theme-archives/shrine-pro-v3.0.0.zip
        Path targetPath = Paths.get(basePath, fileName);

        // 确保目录存在
        Files.createDirectories(targetPath.getParent());

        // 保存文件
        Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Saved to local storage: {}", targetPath);
        return targetPath.toString();
    }

    @Override
    public Path download(String storagePath) throws Exception {
        // 本地存储直接返回路径
        Path path = Paths.get(storagePath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + storagePath);
        }

        return path;
    }

    @Override
    public void delete(String storagePath) throws Exception {
        Path path = Paths.get(storagePath);
        Files.deleteIfExists(path);
        log.info("Deleted from local storage: {}", path);
    }

    @Override
    public String getPresignedUrl(String storagePath, int expireSeconds) {
        // 本地存储返回文件路径
        return "file://" + storagePath;
    }

    @Override
    public boolean exists(String storagePath) {
        return Files.exists(Paths.get(storagePath));
    }
}
