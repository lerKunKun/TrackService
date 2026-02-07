package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 默认值规则
 * 为新增字段提供默认值
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultValueRule {

    /**
     * Section名称
     */
    private String sectionName;

    /**
     * 字段ID
     */
    private String fieldId;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 默认值类型
     */
    private String valueType;

    /**
     * 设置默认值的原因
     */
    private String reason;

    /**
     * 是否必须设置（字段是必填的）
     */
    private Boolean required;
}
