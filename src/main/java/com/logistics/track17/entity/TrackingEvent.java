package com.logistics.track17.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物流事件实体
 */
@Data
public class TrackingEvent {
    private Long id;
    private Long trackingId;
    private LocalDateTime eventTime;
    private String eventDescription;    // 事件描述
    private String eventLocation;       // 事件位置
    private String city;                // 城市
    private String postalCode;          // 邮编
    private Integer providerKey;        // 承运商ID
    private String providerName;        // 承运商名称
    private String eventCode;           // 事件代码
    private String stage;               // 阶段 InfoReceived/InTransit/Delivered/Exception等
    private String subStatus;           // 子状态
    private String timeIso;             // ISO时间字符串
    private LocalDateTime createdAt;
}
