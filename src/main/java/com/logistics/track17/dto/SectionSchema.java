package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Liquid Section Schema数据结构
 * 对应{% schema %}...{% endschema %}块中的JSON结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionSchema {

    /**
     * Section名称
     */
    private String name;

    /**
     * Section类别/标签
     */
    private String class_name;

    /**
     * Section设置列表
     */
    private List<SchemaSetting> settings;

    /**
     * Block类型定义（如果section支持blocks）
     */
    private List<BlockType> blocks;

    /**
     * 预设配置
     */
    private List<Preset> presets;

    /**
     * 是否可用
     */
    private Boolean enabled;

    /**
     * 限制（最大数量等）
     */
    private Integer limit;

    /**
     * 其他自定义属性
     */
    private java.util.Map<String, Object> additionalProperties;

    /**
     * Block类型定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockType {
        private String type;
        private String name;
        private List<SchemaSetting> settings;
        private Integer limit;
    }

    /**
     * 预设配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Preset {
        private String name;
        private String category;
        private java.util.Map<String, Object> settings;
        private List<java.util.Map<String, Object>> blocks;
    }
}
