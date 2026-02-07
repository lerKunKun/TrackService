package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量更新结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateResult {

    /** 更新的模板数量 */
    private int templatesUpdated;

    /** 应用的规则数量 */
    private int rulesApplied;

    /** 更新的section实例数量 */
    private int sectionsUpdated;

    /** 跳过的模板（无需更新） */
    private int templatesSkipped;

    /** 错误信息 */
    private String errorMessage;

    public boolean isSuccessful() {
        return errorMessage == null;
    }
}
