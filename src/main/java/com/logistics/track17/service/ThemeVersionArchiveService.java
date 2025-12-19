package com.logistics.track17.service;

import com.logistics.track17.entity.ThemeVersion;
import com.logistics.track17.mapper.ThemeVersionMapper;
import com.logistics.track17.service.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 主题版本存档服务
 */
@Slf4j
@Service
public class ThemeVersionArchiveService {

    @Autowired
    private ThemeVersionMapper themeVersionMapper;

    @Autowired
    private StorageService storageService;

    /**
     * 存档主题版本
     */
    public ThemeVersion archiveTheme(MultipartFile zipFile, String themeName,
            String version, String uploadedBy) throws Exception {

        // 生成标准文件名
        String fileName = String.format("%s-v%s.zip",
                themeName.replace(" ", "-"),
                version);

        // 上传到存储（MinIO或本地）
        String storagePath = storageService.upload(
                zipFile.getInputStream(),
                fileName,
                "application/zip");

        // 保存到数据库
        ThemeVersion archive = new ThemeVersion();
        archive.setThemeName(themeName);
        archive.setVersion(version);
        archive.setZipFilePath(storagePath);
        archive.setZipFileSize(zipFile.getSize());
        archive.setIsCurrent(false); // 初始不设为当前版本
        archive.setUploadedBy(uploadedBy);
        // uploadedAt 由数据库自动设置

        themeVersionMapper.insert(archive);

        log.info("Archived theme version: {} v{}", themeName, version);
        return archive;
    }

    /**
     * 获取当前版本
     */
    public ThemeVersion getCurrentVersion(String themeName) {
        ThemeVersion version = themeVersionMapper.selectCurrentVersion(themeName);
        if (version == null) {
            throw new RuntimeException("No current version found for theme: " + themeName);
        }
        return version;
    }

    /**
     * 设置为当前版本
     */
    public void markAsCurrentVersion(String themeName, String version) {
        // 清除旧的current标记
        themeVersionMapper.clearCurrentFlag(themeName);

        // 设置新的current标记
        themeVersionMapper.setAsCurrentVersion(themeName, version);

        log.info("Marked as current version: {} v{}", themeName, version);
    }

    /**
     * 获取主题文件（下载到临时目录）
     */
    public Path getThemeFile(String storagePath) throws Exception {
        return storageService.download(storagePath);
    }

    /**
     * 获取版本历史
     */
    public List<ThemeVersion> getVersionHistory(String themeName) {
        return themeVersionMapper.selectByThemeName(themeName);
    }

    /**
     * 根据主题名和版本号获取指定版本
     */
    public ThemeVersion getVersionByThemeAndVersion(String themeName, String version) {
        ThemeVersion themeVersion = themeVersionMapper.selectByThemeAndVersion(themeName, version);
        if (themeVersion == null) {
            throw new RuntimeException("Version not found: " + themeName + " v" + version);
        }
        return themeVersion;
    }

    /**
     * 删除版本
     */
    public void deleteVersion(Long versionId) throws Exception {
        ThemeVersion version = themeVersionMapper.selectById(versionId);
        if (version == null) {
            throw new RuntimeException("Version not found: " + versionId);
        }

        // 删除存储文件
        storageService.delete(version.getZipFilePath());

        // 删除数据库记录
        themeVersionMapper.deleteById(versionId);

        log.info("Deleted version: {} v{}", version.getThemeName(), version.getVersion());
    }
}
