package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知推送日志
 */
@Data
public class NotificationLog {
    private Long id;
    private Long shopId;
    private String alertType;
    private String severity;
    private String source;
    private String title;
    private String content;
    private String dedupKey;
    private String recipientUserid;
    private String sendStatus;
    private String errorMessage;
    private LocalDateTime sentTime;
    private LocalDateTime createdTime;
}
