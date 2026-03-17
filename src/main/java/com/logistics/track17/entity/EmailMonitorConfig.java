package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 邮箱监控配置
 */
@Data
public class EmailMonitorConfig {
    private Long id;
    private String name;
    private String host;
    private Integer port;
    private String protocol;
    private String username;
    private String password;
    private String senderFilter;
    private Integer checkInterval;
    private Boolean isEnabled;
    private LocalDateTime lastCheckTime;
    private String lastCheckStatus;
    private String lastErrorMessage;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // ====== OAuth2 Microsoft Modern Auth ======
    /** 认证类型：PASSWORD / OAUTH2_MICROSOFT */
    private String authType;
    /** OAuth2 访问令牌（1小时有效） */
    private String accessToken;
    /** OAuth2 刷新令牌（长期有效） */
    private String refreshToken;
    /** access_token 过期时间 */
    private LocalDateTime tokenExpiry;
}
