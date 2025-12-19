package com.logistics.track17.service;

import com.logistics.track17.dto.*;
import com.logistics.track17.entity.ThemeMigrationHistory;
import com.logistics.track17.entity.ThemeVersion;
import com.logistics.track17.mapper.ThemeMigrationHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 统一主题迁移服务
 * 编排整个迁移流程
 */
@Slf4j
@Service
public class UnifiedMigrationService {

    @Autowired
    private ThemeVersionArchiveService archiveService;

    @Autowired
    private ThemeGitDiffAnalyzer gitAnalyzer;

    @Autowired
    private IntelligentRuleEngine ruleEngine;

    @Autowired
    private ProductTemplateUpdater templateUpdater;

    @Autowired
    private ThemeMigrationHistoryMapper historyMapper;

    // 内存中的会话存储（生产环境应使用Redis）
    private final Map<Long, MigrationSession> sessionStore = new ConcurrentHashMap<>();
    private final AtomicLong sessionIdGenerator = new AtomicLong(1);

    /**
     * 开始迁移流程
     * 1. 存档新版本（或使用已有版本）
     * 2. Git Diff分析
     * 3. 智能规则生成
     * 4. 返回待确认的session
     */
    public MigrationSession startMigration(MultipartFile newZip,
            String themeName,
            String newVersion,
            String uploadedBy) throws Exception {

        log.info("Starting migration for theme: {} to version: {}", themeName, newVersion);

        // 1. 获取当前版本
        ThemeVersion currentVersion = archiveService.getCurrentVersion(themeName);
        String fromVersion = currentVersion.getVersion();

        log.info("Migrating from {} to {}", fromVersion, newVersion);

        // 2. 检查新版本是否已存在
        ThemeVersion newVersionRecord;
        try {
            newVersionRecord = archiveService.getVersionByThemeAndVersion(themeName, newVersion);
            log.info("Version {} already exists, reusing existing record", newVersion);
        } catch (Exception e) {
            // 版本不存在，需要存档
            log.info("Version {} not found, archiving new version", newVersion);
            newVersionRecord = archiveService.archiveTheme(
                    newZip, themeName, newVersion, uploadedBy);
        }

        // 3. 下载两个版本的ZIP文件
        Path oldZip = archiveService.getThemeFile(currentVersion.getZipFilePath());
        Path newZipPath = archiveService.getThemeFile(newVersionRecord.getZipFilePath());

        // 4. 执行Git Diff分析
        ThemeDiffResult diffResult = gitAnalyzer.analyze(oldZip, newZipPath);

        // 5. 智能生成迁移规则
        MigrationRuleSuggestion suggestedRules = ruleEngine.inferRules(diffResult);

        // 6. 创建迁移会话
        MigrationSession session = MigrationSession.builder()
                .id(sessionIdGenerator.getAndIncrement())
                .themeName(themeName)
                .fromVersion(fromVersion)
                .toVersion(newVersion)
                .diffResult(diffResult)
                .suggestedRules(suggestedRules)
                .newThemeZipPath(newVersionRecord.getZipFilePath())
                .createdAt(LocalDateTime.now())
                .status("PENDING")
                .build();

        // 7. 保存session
        sessionStore.put(session.getId(), session);

        log.info("Migration session created: {}", session.getId());

        return session;
    }

    /**
     * 执行迁移
     * 用户确认规则后调用
     */
    public MigrationResult executeMigration(Long sessionId,
            MigrationRuleSuggestion confirmedRules,
            String executedBy) throws Exception {

        log.info("Executing migration for session: {}", sessionId);

        // 1. 获取session
        MigrationSession session = sessionStore.get(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }

        // 2. 更新session状态
        session.setStatus("EXECUTING");
        session.setSuggestedRules(confirmedRules); // 使用用户确认的规则

        // 3. 创建历史记录
        ThemeMigrationHistory history = new ThemeMigrationHistory();
        history.setThemeName(session.getThemeName());
        history.setFromVersion(session.getFromVersion());
        history.setToVersion(session.getToVersion());
        history.setStatus("ANALYZING");
        history.setExecutedBy(executedBy);
        history.setExecutedAt(LocalDateTime.now());
        historyMapper.insert(history);

        // 创建备份路径
        Path backupPath = null;
        Path oldThemeExtracted = null;
        Path newThemeExtracted = null;

        try {
            // 4. 创建当前版本备份
            ThemeVersion oldVersion = archiveService.getCurrentVersion(session.getThemeName());
            backupPath = createBackup(oldVersion);
            log.info("Created backup at: {}", backupPath);

            // 5. 解压旧版本主题（读取product.*.json）
            Path oldThemeZip = archiveService.getThemeFile(oldVersion.getZipFilePath());
            oldThemeExtracted = extractTheme(oldThemeZip);
            log.info("Extracted old theme to: {}", oldThemeExtracted);

            // 6. 解压新版本主题（写入更新后的product.*.json）
            Path newThemeZip = archiveService.getThemeFile(session.getNewThemeZipPath());
            newThemeExtracted = extractTheme(newThemeZip);
            log.info("Extracted new theme to: {}", newThemeExtracted);

            // 7. 从旧版本迁移模板到新版本
            BatchUpdateResult updateResult = templateUpdater.migrateTemplates(
                    oldThemeExtracted, // 从旧版本读取
                    newThemeExtracted, // 写入新版本
                    confirmedRules);

            // 8. 重新打包新版本为ZIP
            Path updatedZipPath = repackTheme(newThemeExtracted, session.getNewThemeZipPath());
            log.info("Repacked new theme to: {}", updatedZipPath);

            // 9. 标记新版本为current
            archiveService.markAsCurrentVersion(session.getThemeName(), session.getToVersion());

            // 10. 更新历史记录
            history.setStatus("SUCCESS");
            history.setTemplatesUpdated(updateResult.getTemplatesUpdated());
            history.setCompletedAt(LocalDateTime.now());
            historyMapper.updateCompletion(history);

            // 11. 更新session
            session.setStatus("COMPLETED");

            log.info("Migration completed successfully. Migrated {} templates from old to new version",
                    updateResult.getTemplatesUpdated());

            return MigrationResult.builder()
                    .success(true)
                    .historyId(history.getId())
                    .templatesUpdated(updateResult.getTemplatesUpdated())
                    .message("Migration completed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Migration failed: {}", e.getMessage(), e);

            // 尝试恢复备份
            if (backupPath != null) {
                try {
                    restoreBackup(backupPath, session.getThemeName());
                    log.info("Restored backup successfully");
                } catch (Exception restoreEx) {
                    log.error("Failed to restore backup", restoreEx);
                }
            }

            // 更新历史记录
            historyMapper.updateStatus(history.getId(), "FAILED", e.getMessage());

            session.setStatus("FAILED");

            throw e;
        } finally {
            // 清理临时目录
            if (oldThemeExtracted != null) {
                try {
                    deleteDirectory(oldThemeExtracted);
                    log.debug("Cleaned up old theme temp directory");
                } catch (Exception e) {
                    log.warn("Failed to cleanup old theme temp directory: {}", e.getMessage());
                }
            }
            if (newThemeExtracted != null) {
                try {
                    deleteDirectory(newThemeExtracted);
                    log.debug("Cleaned up new theme temp directory");
                } catch (Exception e) {
                    log.warn("Failed to cleanup new theme temp directory: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 获取会话
     */
    public MigrationSession getSession(Long sessionId) {
        MigrationSession session = sessionStore.get(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }
        return session;
    }

    /**
     * 解压主题ZIP到临时目录
     */
    private Path extractTheme(Path zipFile) throws Exception {
        Path tempDir = Files.createTempDirectory("theme-extracted-");
        log.debug("Extracting ZIP {} to {}", zipFile, tempDir);

        try (org.apache.commons.compress.archivers.zip.ZipFile zip = new org.apache.commons.compress.archivers.zip.ZipFile(
                zipFile.toFile())) {

            java.util.Enumeration<org.apache.commons.compress.archivers.zip.ZipArchiveEntry> entries = zip.getEntries();

            while (entries.hasMoreElements()) {
                org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry = entries.nextElement();
                File file = tempDir.resolve(entry.getName()).toFile();

                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (java.io.InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }

        log.info("Successfully extracted ZIP to {}", tempDir);
        return tempDir;
    }

    /**
     * 重新打包主题为ZIP
     */
    private Path repackTheme(Path extractedDir, String originalZipPath) throws Exception {
        // 创建临时ZIP文件
        Path tempZip = Files.createTempFile("theme-repacked-", ".zip");
        log.debug("Repacking theme from {} to {}", extractedDir, tempZip);

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(tempZip.toFile()))) {

            Files.walk(extractedDir).forEach(path -> {
                try {
                    if (!Files.isDirectory(path)) {
                        String entryName = extractedDir.relativize(path).toString()
                                .replace("\\", "/"); // Windows路径转Unix

                        zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to add entry to ZIP", e);
                }
            });
        }

        // 替换原始ZIP文件
        Path originalZip = java.nio.file.Paths.get(originalZipPath);
        Files.copy(tempZip, originalZip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.delete(tempZip);

        log.info("Successfully repacked theme to {}", originalZip);
        return originalZip;
    }

    /**
     * 创建当前版本的备份
     */
    private Path createBackup(ThemeVersion version) throws Exception {
        Path originalZip = java.nio.file.Paths.get(version.getZipFilePath());
        if (!Files.exists(originalZip)) {
            throw new RuntimeException("Original ZIP not found: " + originalZip);
        }

        // 创建备份文件名
        String backupFileName = String.format("%s-v%s-backup-%d.zip",
                version.getThemeName().replace(" ", "-"),
                version.getVersion(),
                System.currentTimeMillis());

        Path backupPath = originalZip.getParent().resolve("backups").resolve(backupFileName);
        Files.createDirectories(backupPath.getParent());

        // 复制文件
        Files.copy(originalZip, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        log.info("Created backup: {}", backupPath);

        return backupPath;
    }

    /**
     * 从备份恢复
     */
    private void restoreBackup(Path backupPath, String themeName) throws Exception {
        if (!Files.exists(backupPath)) {
            throw new RuntimeException("Backup not found: " + backupPath);
        }

        // 获取当前版本记录
        ThemeVersion currentVersion = archiveService.getCurrentVersion(themeName);
        Path targetZip = java.nio.file.Paths.get(currentVersion.getZipFilePath());

        // 恢复备份
        Files.copy(backupPath, targetZip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        log.info("Restored backup from {} to {}", backupPath, targetZip);
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(Path directory) throws Exception {
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

    /**
     * 获取迁移后的主题ZIP路径（用于下载）
     */
    public Path getMigratedThemePath(Long historyId) throws Exception {
        // 1. 获取迁移历史记录
        ThemeMigrationHistory history = historyMapper.selectById(historyId);
        if (history == null) {
            throw new RuntimeException("Migration history not found: " + historyId);
        }

        // 2. 检查迁移是否成功
        if (!"SUCCESS".equals(history.getStatus())) {
            throw new RuntimeException("Migration not successful. Status: " + history.getStatus());
        }

        // 3. 获取新版本的ZIP路径
        ThemeVersion newVersion = archiveService.getVersionByThemeAndVersion(
                history.getThemeName(),
                history.getToVersion());

        Path zipPath = java.nio.file.Paths.get(newVersion.getZipFilePath());

        // 4. 验证文件存在
        if (!Files.exists(zipPath)) {
            throw new RuntimeException("Theme ZIP file not found: " + zipPath);
        }

        return zipPath;
    }
}
