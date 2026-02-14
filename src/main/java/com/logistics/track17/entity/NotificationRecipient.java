package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知接收人配置
 */
@Data
public class NotificationRecipient {
    private Long id;
    private String name;
    private String dingtalkUserid;
    private String alertTypes;
    private Boolean isEnabled;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    /**
     * 判断是否订阅了指定的Alert类型
     */
    public boolean isSubscribed(String alertType) {
        if (alertTypes == null || "ALL".equalsIgnoreCase(alertTypes)) {
            return true;
        }
        for (String type : alertTypes.split(",")) {
            if (type.trim().equalsIgnoreCase(alertType)) {
                return true;
            }
        }
        return false;
    }
}
