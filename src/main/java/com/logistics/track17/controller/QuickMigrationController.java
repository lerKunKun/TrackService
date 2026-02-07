package com.logistics.track17.controller;

import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.ThemeVersion;
import com.logistics.track17.service.DeepDiffAnalyzer;
import com.logistics.track17.service.MigrationRuleLoader;
import com.logistics.track17.service.ProductTemplateUpdater;
import com.logistics.track17.service.ThemeVersionArchiveService;
import com.logistics.track17.dto.ExecutableMigrationRules;
import com.logistics.track17.dto.BatchUpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 一键主题迁移控制器
 * 面向普通用户，提供简化的迁移流程
 */
@Slf4j
@RestController
@RequestMapping("/theme/quick-migration")
public class QuickMigrationController {

    @Autowired
    private ThemeVersionArchiveService archiveService;

    @Autowired
    private DeepDiffAnalyzer deepDiffAnalyzer;

    @Autowired
    private MigrationRuleLoader ruleLoader;

    @Autowired
    private ProductTemplateUpdater templateUpdater;

    /**
     * 一键迁移
     * 接收用户的当前主题ZIP，自动分析并迁移到目标版本
     */
    @PostMapping("/execute")
    public Result<Map<String, Object>> oneClickMigration(
            @RequestParam("currentThemeZip") MultipartFile currentThemeZip,
            @RequestParam("themeName") String themeName,
            @RequestParam("targetVersion") String targetVersion,
            HttpServletRequest request) {

        try {
            String username = (String) request.getAttribute("username");
            if (username == null) {
                username = "guest";
            }

            log.info("User {} starting one-click migration: {} -> {}",
                    username, themeName, targetVersion);

            // 1. 验证目标版本是否存在
            ThemeVersion targetVersionRecord = archiveService.getVersionByThemeAndVersion(
                    themeName, targetVersion);
            if (targetVersionRecord == null) {
                return Result.error("目标版本不存在: " + targetVersion);
            }

            // 2. 获取当前版本（假设是最新的current版本）
            ThemeVersion currentVersion = archiveService.getCurrentVersion(themeName);
            String fromVersion = currentVersion.getVersion();

            // 3. 检查是否已有迁移规则
            boolean rulesExist = ruleLoader.rulesExist(themeName, fromVersion, targetVersion);

            Path oldThemeExtracted = null;
            Path newThemeExtracted = null;
            Path migratedThemePath = null;

            try {
                // 4. 解压当前上传的主题（用户的自定义配置）
                oldThemeExtracted = extractUploadedTheme(currentThemeZip);

                // 5. 如果规则不存在，先进行深度分析
                if (!rulesExist) {
                    log.info("Rules not found, performing deep diff analysis...");

                    // 获取目标版本的ZIP
                    Path targetVersionZip = archiveService.getThemeFile(
                            targetVersionRecord.getZipFilePath());
                    newThemeExtracted = extractTheme(targetVersionZip);

                    // 深度分析并生成规则
                    deepDiffAnalyzer.analyzeAndSaveRules(
                            themeName,
                            fromVersion,
                            targetVersion,
                            oldThemeExtracted,
                            newThemeExtracted,
                            username);

                    log.info("Deep diff analysis completed");
                } else {
                    // 如果规则已存在，只需解压目标版本
                    Path targetVersionZip = archiveService.getThemeFile(
                            targetVersionRecord.getZipFilePath());
                    newThemeExtracted = extractTheme(targetVersionZip);
                }

                // 6. 加载规则
                ExecutableMigrationRules rules = ruleLoader.loadRules(
                        themeName, fromVersion, targetVersion);

                log.info("Loaded migration rules: {}", rules.getStatistics());

                // 7. 执行迁移
                BatchUpdateResult migrateResult = templateUpdater.migrateTemplates(
                        oldThemeExtracted, // 用户上传的主题（源）
                        newThemeExtracted, // 目标版本主题
                        rules);

                // 8. 打包迁移后的主题
                migratedThemePath = packageMigratedTheme(
                        newThemeExtracted,
                        themeName,
                        targetVersion);

                // 9. 返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "迁移成功");
                result.put("templatesUpdated", migrateResult.getTemplatesUpdated());
                result.put("rulesApplied", migrateResult.getRulesApplied());
                result.put("downloadPath", migratedThemePath.toString());
                result.put("themeName", themeName);
                result.put("targetVersion", targetVersion);

                log.info("One-click migration completed successfully");

                return Result.success(result);

            } finally {
                // 清理临时目录
                if (oldThemeExtracted != null && Files.exists(oldThemeExtracted)) {
                    deleteDirectory(oldThemeExtracted);
                }
                // 注意：newThemeExtracted 不删除，因为它是打包后的主题
            }

        } catch (Exception e) {
            log.error("One-click migration failed", e);
            return Result.error("迁移失败: " + e.getMessage());
        }
    }

    /**
     * 下载迁移后的主题
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadMigratedTheme(
            @RequestParam("path") String downloadPath) {

        try {
            Path themeZipPath = Paths.get(downloadPath);

            if (!Files.exists(themeZipPath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(themeZipPath.toFile());
            String filename = themeZipPath.getFileName().toString();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            log.error("Failed to download migrated theme", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 解压上传的主题ZIP
     */
    private Path extractUploadedTheme(MultipartFile zipFile) throws Exception {
        // 保存到临时文件
        Path tempZip = Files.createTempFile("uploaded-theme-", ".zip");
        zipFile.transferTo(tempZip.toFile());

        // 解压
        Path extracted = extractTheme(tempZip);

        // 删除临时ZIP
        Files.delete(tempZip);

        return extracted;
    }

    /**
     * 解压主题ZIP
     */
    private Path extractTheme(Path zipFile) throws Exception {
        Path tempDir = Files.createTempDirectory("theme-extracted-");

        try (org.apache.commons.compress.archivers.zip.ZipFile zip = new org.apache.commons.compress.archivers.zip.ZipFile(
                zipFile.toFile())) {

            java.util.Enumeration<org.apache.commons.compress.archivers.zip.ZipArchiveEntry> entries = zip.getEntries();

            while (entries.hasMoreElements()) {
                org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry = entries.nextElement();
                java.io.File file = tempDir.resolve(entry.getName()).toFile();

                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (java.io.InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, file.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }

        return tempDir;
    }

    /**
     * 打包迁移后的主题
     */
    private Path packageMigratedTheme(Path themeDir, String themeName, String version)
            throws Exception {

        // 直接创建临时文件（文件名会自动生成）
        Path outputZip = Files.createTempFile(
                themeName.replace(" ", "-") + "-v" + version + "-migrated-",
                ".zip");

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(outputZip.toFile()))) {

            Files.walk(themeDir).forEach(path -> {
                try {
                    if (!Files.isDirectory(path)) {
                        String entryName = themeDir.relativize(path).toString()
                                .replace("\\", "/");

                        zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to add entry to ZIP", e);
                }
            });
        }

        return outputZip;
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (java.util.stream.Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            log.warn("Failed to delete: {}", path, e);
                        }
                    });
        }
    }
}
