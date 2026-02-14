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
}
