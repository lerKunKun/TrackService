package com.logistics.track17.controller;

import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.ThemeVersion;
import com.logistics.track17.service.ThemeVersionArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * 主题版本管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/theme/version")
public class ThemeVersionController {

    @Autowired
    private ThemeVersionArchiveService archiveService;

    @Autowired
    private com.logistics.track17.service.DeepDiffAnalyzer deepDiffAnalyzer;

    @Autowired
    private com.logistics.track17.mapper.ThemeMigrationRuleMapper ruleMapper;

    /**
     * 上传并存档主题版本
     */
    @PostMapping("/archive")
    public Result archiveTheme(
            @RequestParam("file") MultipartFile zipFile,
            @RequestParam("themeName") String themeName,
            @RequestParam("version") String version,
            HttpServletRequest request) {

        try {
            String uploadedBy = (String) request.getAttribute("username");
            if (uploadedBy == null) {
                uploadedBy = "system";
            }

            ThemeVersion archived = archiveService.archiveTheme(
                    zipFile, themeName, version, uploadedBy);

            return Result.success(archived);

        } catch (Exception e) {
            log.error("Failed to archive theme", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取主题的版本历史
     */
    @GetMapping("/history/{themeName}")
    public Result getVersionHistory(@PathVariable String themeName) {
        try {
            List<ThemeVersion> history = archiveService.getVersionHistory(themeName);
            return Result.success(history);

        } catch (Exception e) {
            log.error("Failed to get version history", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前版本
     */
    @GetMapping("/current/{themeName}")
    public Result getCurrentVersion(@PathVariable String themeName) {
        try {
            ThemeVersion current = archiveService.getCurrentVersion(themeName);
            return Result.success(current);

        } catch (Exception e) {
            log.error("Failed to get current version", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 设置为当前版本
     */
    @PostMapping("/set-current")
    public Result setAsCurrentVersion(
            @RequestParam("themeName") String themeName,
            @RequestParam("version") String version) {

        try {
            archiveService.markAsCurrentVersion(themeName, version);
            return Result.success("已设置为当前版本");

        } catch (Exception e) {
            log.error("Failed to set current version", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除版本
     */
    @DeleteMapping("/{versionId}")
    public Result deleteVersion(@PathVariable Long versionId) {
        try {
            archiveService.deleteVersion(versionId);
            return Result.success("删除成功");

        } catch (Exception e) {
            log.error("Failed to delete version", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 触发深度差异分析
     * 分析两个版本之间的schema变化并生成迁移规则
     */
    @PostMapping("/analyze-diff")
    public Result analyzeDiff(
            @RequestParam("themeName") String themeName,
            @RequestParam("fromVersion") String fromVersion,
            @RequestParam("toVersion") String toVersion,
            HttpServletRequest request) {

        try {
            String username = (String) request.getAttribute("username");
            if (username == null) {
                username = "system";
            }

            log.info("Starting deep analysis: {} {} -> {}", themeName, fromVersion, toVersion);

            // 获取两个版本的主题文件
            ThemeVersion oldVersion = archiveService.getVersionByThemeAndVersion(themeName, fromVersion);
            ThemeVersion newVersion = archiveService.getVersionByThemeAndVersion(themeName, toVersion);

            if (oldVersion == null) {
                return Result.error("Source version not found: " + fromVersion);
            }
            if (newVersion == null) {
                return Result.error("Target version not found: " + toVersion);
            }

            // 获取解压路径
            java.nio.file.Path oldThemePath = archiveService.getThemeFile(oldVersion.getZipFilePath()).getParent();
            java.nio.file.Path newThemePath = archiveService.getThemeFile(newVersion.getZipFilePath()).getParent();

            // 临时解压主题文件以进行分析
            java.nio.file.Path oldExtracted = extractThemeForAnalysis(oldVersion.getZipFilePath());
            java.nio.file.Path newExtracted = extractThemeForAnalysis(newVersion.getZipFilePath());

            try {
                // 执行深度分析
                deepDiffAnalyzer.analyzeAndSaveRules(
                        themeName,
                        fromVersion,
                        toVersion,
                        oldExtracted,
                        newExtracted,
                        username);

                // 查询生成的规则数量
                java.util.List<com.logistics.track17.entity.ThemeMigrationRule> rules = ruleMapper
                        .selectByVersions(themeName, fromVersion, toVersion);

                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("rulesGenerated", rules.size());

                // 统计规则类型
                java.util.Map<String, Long> breakdown = rules.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                com.logistics.track17.entity.ThemeMigrationRule::getRuleType,
                                java.util.stream.Collectors.counting()));
                result.put("breakdown", breakdown);

                return Result.success(result);

            } finally {
                // 清理临时文件
                cleanupExtractedTheme(oldExtracted);
                cleanupExtractedTheme(newExtracted);
            }

        } catch (Exception e) {
            log.error("Deep analysis failed", e);
            return Result.error("Analysis failed: " + e.getMessage());
        }
    }

    /**
     * 查看迁移规则
     */
    @GetMapping("/migration-rules")
    public Result getMigrationRules(
            @RequestParam("themeName") String themeName,
            @RequestParam("fromVersion") String fromVersion,
            @RequestParam("toVersion") String toVersion,
            @RequestParam(value = "ruleType", required = false) String ruleType) {

        try {
            java.util.List<com.logistics.track17.entity.ThemeMigrationRule> rules;

            if (ruleType != null && !ruleType.isEmpty()) {
                rules = ruleMapper.selectByVersionsAndType(themeName, fromVersion, toVersion, ruleType);
            } else {
                rules = ruleMapper.selectByVersions(themeName, fromVersion, toVersion);
            }

            return Result.success(rules);

        } catch (Exception e) {
            log.error("Failed to get migration rules", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 临时解压主题用于分析
     */
    private java.nio.file.Path extractThemeForAnalysis(String zipFilePath) throws Exception {
        java.nio.file.Path zipPath = java.nio.file.Paths.get(zipFilePath);
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("theme-analysis-");

        // 使用Apache Commons Compress解压
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new java.io.FileInputStream(zipPath.toFile()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                java.nio.file.Path targetPath = tempDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    java.nio.file.Files.createDirectories(targetPath);
                } else {
                    java.nio.file.Files.createDirectories(targetPath.getParent());
                    java.nio.file.Files.copy(zis, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }

        return tempDir;
    }

    /**
     * 清理解压的临时文件
     */
    private void cleanupExtractedTheme(java.nio.file.Path path) {
        if (path != null && java.nio.file.Files.exists(path)) {
            try {
                java.nio.file.Files.walk(path)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                java.nio.file.Files.delete(p);
                            } catch (Exception e) {
                                log.warn("Failed to delete: {}", p, e);
                            }
                        });
            } catch (Exception e) {
                log.error("Failed to cleanup temp directory: {}", path, e);
            }
        }
    }
}
