package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Data
public class LoginLog {

    private Long id;
    private Long userId;
    private String username;

    /**
     * 登录类型：PASSWORD / DINGTALK / LOGOUT
     */
    private String loginType;

    /**
     * 登录结果：SUCCESS / FAILURE
     */
    private String loginResult;

    private String ipAddress;
    private String userAgent;
    private String device;
    private String browser;
    private String os;
    private String errorMsg;
    private LocalDateTime createdAt;
}
