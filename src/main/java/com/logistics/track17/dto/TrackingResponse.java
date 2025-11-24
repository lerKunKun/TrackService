package com.logistics.track17.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运单响应DTO
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackingResponse {
    private Long id;
    private String trackingNumber;
    private String carrierCode;
    private String carrierName;
    private Integer carrierId;
    private String trackStatus;          // 主状态: InfoReceived/InTransit/Delivered/Exception
    private String subStatus;            // 子状态: Delivered_Other, InTransit_PickedUp等
    private String subStatusDescr;       // 子状态描述
    private String source;               // 来源: manual/batch_import/shopify等
    private String remarks;              // 备注

    // 时效信息
    private Integer daysOfTransit;       // 运输天数
    private Integer daysAfterLastUpdate; // 距离最后更新天数

    // 最新事件信息
    private LocalDateTime latestEventTime;
    private String latestEventDesc;
    private String latestEventLocation;

    // 地址信息
    private String originCountry;        // 始发国家
    private String destinationCountry;   // 目的国家

    // 时间节点
    private LocalDateTime pickupTime;    // 揽收时间
    private LocalDateTime deliveredTime; // 签收时间
    private LocalDateTime lastSyncAt;
    private LocalDateTime nextSyncAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 关联订单信息
    private Long orderId;
    private String orderNumber;

    // 详情页包含的物流事件
    private List<TrackingEventResponse> events;
}
