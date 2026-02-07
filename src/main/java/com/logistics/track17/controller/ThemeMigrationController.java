package com.logistics.track17.controller;

import com.logistics.track17.dto.Result;
import com.logistics.track17.dto.MigrationResult;
import com.logistics.track17.dto.ExecutableMigrationRules;
import com.logistics.track17.dto.MigrationSession;
import com.logistics.track17.service.UnifiedMigrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 主题迁移Controller
 */
@Slf4j
@RestController
@RequestMapping("/theme/migration")
public class ThemeMigrationController {

    @Autowired
    private UnifiedMigrationService migrationService;

    /**
     * 开始迁移流程
     * 上传新版本ZIP，分析差异，生成规则建议
     */
    @PostMapping("/start")
    public Result startMigration(
            @RequestParam("file") MultipartFile zipFile,
            @RequestParam("themeName") String themeName,
            @RequestParam("newVersion") String newVersion,
            HttpServletRequest request) {

        try {
            String uploadedBy = (String) request.getAttribute("username");
            if (uploadedBy == null) {
                uploadedBy = "system";
            }

            MigrationSession session = migrationService.startMigration(
                    zipFile, themeName, newVersion, uploadedBy);

            return Result.success(session);

        } catch (Exception e) {
            log.error("Failed to start migration", e);
            return Result.error("启动迁移失败: " + e.getMessage());
        }
    }

    /**
     * 执行迁移
     * 用户确认规则后调用（可选传入规则，不传则使用数据库中的规则）
     */
    @PostMapping("/execute")
    public Result executeMigration(
            @RequestParam("sessionId") Long sessionId,
            @RequestBody(required = false) ExecutableMigrationRules confirmedRules,
            HttpServletRequest request) {

        try {
            String executedBy = (String) request.getAttribute("username");
            if (executedBy == null) {
                executedBy = "system";
            }

            MigrationResult result = migrationService.executeMigration(
                    sessionId, confirmedRules, executedBy);

            return Result.success(result);

        } catch (Exception e) {
            log.error("Failed to execute migration", e);
            return Result.error("执行迁移失败: " + e.getMessage());
        }
    }

    /**
     * 获取迁移会话
     */
    @GetMapping("/session/{sessionId}")
    public Result getSession(@PathVariable Long sessionId) {
        try {
            MigrationSession session = migrationService.getSession(sessionId);
            return Result.success(session);

        } catch (Exception e) {
            log.error("Failed to get session", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 下载迁移后的主题ZIP
     */
    @GetMapping("/download/{historyId}")
    public ResponseEntity<Resource> downloadMigratedTheme(
            @PathVariable Long historyId,
            @RequestAttribute("currentUser") String username) {
        try {
            log.info("User {} downloading migrated theme for history {}", username, historyId);

            // 获取迁移后的主题文件路径
            Path themeZipPath = migrationService.getMigratedThemePath(historyId);

            // 创建文件资源
            Resource resource = new FileSystemResource(themeZipPath.toFile());

            if (!resource.exists()) {
                log.error("Theme ZIP not found: {}", themeZipPath);
                return ResponseEntity.notFound().build();
            }

            // 构建文件名
            String filename = themeZipPath.getFileName().toString();

            // 返回文件下载响应
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
}
