package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 钉钉同步日志实体
 * 记录每次同步操作的详细信息
 */
@Data
public class DingtalkSyncLog {
    /**
     * 日志ID
     */
    private Long id;

    /**
     * 同步类型
     * DEPT - 部门同步
     * USER - 用户同步
     * FULL - 全量同步
     */
    private String syncType;

    /**
     * 同步模式
     * MANUAL - 手动触发
     * AUTO - 自动定时
     */
    private String syncMode;

    /**
     * 同步状态
     * SUCCESS - 成功
     * FAILED - 失败
     * PARTIAL - 部分成功
     */
    private String status;

    /**
     * 总数
     */
    private Integer totalCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failedCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
