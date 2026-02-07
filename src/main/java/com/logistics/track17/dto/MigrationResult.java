package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 迁移执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationResult {

    /** 是否成功 */
    private boolean success;

    /** 历史记录ID */
    private Long historyId;

    /** 更新的模板数量 */
    private int templatesUpdated;

    /** 消息 */
    private String message;

    /** 错误详情 */
    private String errorDetails;
}
