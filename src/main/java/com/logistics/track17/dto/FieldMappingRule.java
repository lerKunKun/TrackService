package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段映射规则
 * 用于将旧字段映射到新字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMappingRule {

    /**
     * Section名称
     */
    private String sectionName;

    /**
     * 旧字段ID
     */
    private String oldFieldId;

    /**
     * 新字段ID
     */
    private String newFieldId;

    /**
     * 置信度
     */
    private String confidence;

    /**
     * 相似度分数（0.0-1.0）
     */
    private Double similarity;

    /**
     * 映射原因/依据
     */
    private String reason;

    /**
     * 是否需要值转换
     */
    private Boolean needsValueConversion;

    /**
     * 值转换器类型（如果需要）
     */
    private String valueConverterType;
}
