package com.logistics.track17.dto;

import lombok.Data;

/**
 * Schema变更（简化版）
 */
@Data
public class SchemaChanges {
    private boolean hasChanges;
    // 未来可以扩展为详细的schema字段对比
}
