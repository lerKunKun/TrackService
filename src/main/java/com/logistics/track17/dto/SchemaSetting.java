package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Schema Setting定义
 * 对应schema中的单个setting字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaSetting {

    /**
     * 字段ID（唯一标识）
     */
    private String id;

    /**
     * 字段类型：text, textarea, image, url, select, checkbox, radio, range, color等
     */
    private String type;

    /**
     * 显示标签
     */
    private String label;

    /**
     * 默认值
     */
    private Object default_value;

    /**
     * 占位符文本
     */
    private String placeholder;

    /**
     * 帮助文本/说明
     */
    private String info;

    /**
     * 选项列表（用于select/radio类型）
     */
    private List<Option> options;

    /**
     * 最小值（用于range类型）
     */
    private Integer min;

    /**
     * 最大值（用于range类型）
     */
    private Integer max;

    /**
     * 步长（用于range类型）
     */
    private Integer step;

    /**
     * 单位（用于range类型）
     */
    private String unit;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 其他自定义属性
     */
    private Map<String, Object> additionalProperties;

    /**
     * 选项定义（用于select/radio类型）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private String value;
        private String label;
        private String group;
    }
}
