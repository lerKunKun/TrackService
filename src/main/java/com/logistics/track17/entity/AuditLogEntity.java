package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审计日志实体
 */
@Data
public class AuditLogEntity {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 模块名称
     */
    private String module;

    /**
     * 方法名
     */
    private String method;

    /**
     * 请求参数
     */
    private String params;

    /**
     * 操作结果：SUCCESS/FAILURE
     */
    private String result;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 执行时长（毫秒）
     */
    private Integer executionTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
