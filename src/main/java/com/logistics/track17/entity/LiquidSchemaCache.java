package com.logistics.track17.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Liquid Schema缓存Entity
 * 对应liquid_schema_cache表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiquidSchemaCache {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 主题名称
     */
    private String themeName;

    /**
     * 版本号
     */
    private String version;

    /**
     * 文件路径，如: sections/hero.liquid
     */
    private String filePath;

    /**
     * Schema中的name字段
     */
    private String sectionName;

    /**
     * Section类型标识
     */
    private String sectionType;

    /**
     * 完整的schema JSON
     */
    private String schemaJson;

    /**
     * Settings数量
     */
    private Integer settingsCount;

    /**
     * Settings的MD5哈希
     */
    private String settingsHash;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
