package com.logistics.track17.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运单实体
 */
@Data
public class TrackingNumber {
    private Long id;
    private Long userId;
    private Long parcelId;
    private String trackingNumber;
    private String carrierCode;
    private String carrierName;
    private Integer carrierId; // 17Track承运商ID
    private String source; // shopify / manual / api / batch_import
    private String remarks; // 备注
    private String trackStatus; // InfoReceived / InTransit / Delivered / Exception
    private String subStatus; // 子状态如 Delivered_Other, InTransit_PickedUp
    private String subStatusDescr; // 子状态描述
    private Integer daysOfTransit; // 运输天数
    private Integer daysAfterLastUpdate;// 距离最后更新天数
    private LocalDateTime latestEventTime; // 最新事件时间
    private String latestEventDesc; // 最新事件描述
    private String latestEventLocation; // 最新事件地点
    private LocalDateTime pickupTime; // 揽收时间
    private LocalDateTime deliveredTime;// 签收时间
    private LocalDateTime lastSyncAt;
    private LocalDateTime nextSyncAt;
    private String rawStatus; // JSON格式存储原始状态
    private String destinationCountry; // 目的国家
    private String originCountry; // 始发国家
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version; // 版本号（乐观锁）
    private LocalDateTime deletedAt; // 删除时间（软删除）
}
