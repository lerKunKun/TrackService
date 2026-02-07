package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 迁移会话DTO
 * 用于在前端确认规则前保持迁移状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationSession {

    /** 会话ID */
    private Long id;

    /** 主题名称 */
    private String themeName;

    /** 源版本 */
    private String fromVersion;

    /** 目标版本 */
    private String toVersion;

    /** Diff分析结果 */
    private ThemeDiffResult diffResult;

    /** 建议的迁移规则 */
    private MigrationRuleSuggestion suggestedRules;

    /** 新主题ZIP路径 */
    private String newThemeZipPath;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 状态 */
    private String status; // PENDING, REVIEWED, EXECUTING, COMPLETED
}
