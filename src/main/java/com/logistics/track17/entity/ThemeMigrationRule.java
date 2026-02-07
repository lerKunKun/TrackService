package com.logistics.track17.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 主题迁移规则Entity
 * 对应theme_migration_rules表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeMigrationRule {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 主题名称
     */
    private String themeName;

    /**
     * 源版本号
     */
    private String fromVersion;

    /**
     * 目标版本号
     */
    private String toVersion;

    /**
     * 规则类型: SECTION_RENAME, FIELD_MAPPING, DEFAULT_VALUE
     */
    private String ruleType;

    /**
     * Section名称（对于字段映射和默认值规则）
     */
    private String sectionName;

    /**
     * JSON格式的规则详情
     */
    private String ruleJson;

    /**
     * 置信度: CONFIRMED, HIGH, MEDIUM, LOW
     */
    private String confidence;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
