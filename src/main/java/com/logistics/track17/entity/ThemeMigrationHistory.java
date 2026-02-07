package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 主题迁移历史实体
 */
@Data
public class ThemeMigrationHistory {

    private Long id;

    /** 主题名称 */
    private String themeName;

    /** 源版本 */
    private String fromVersion;

    /** 目标版本 */
    private String toVersion;

    /** 旧主题ID（Shopify） */
    private String oldThemeId;

    /** 新主题ID（Shopify） */
    private String newThemeId;

    /** 迁移状态: PENDING, ANALYZING, SUCCESS, FAILED */
    private String status;

    /** 更新的模板数量 */
    private Integer templatesUpdated;

    /** 应用的规则 JSON格式 */
    private String rulesApplied;

    /** 错误信息 */
    private String errorMessage;

    /** 执行用户 */
    private String executedBy;

    /** 执行时间 */
    private LocalDateTime executedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;
}
