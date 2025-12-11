package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 履约（运单）实体
 */
@Data
public class Fulfillment {
    private Long id;
    private Long orderId;
    private Long shopId;
    private Long shopifyFulfillmentId;

    // 物流信息
    private String trackingNumber;
    private String trackingCompany;
    private String trackingUrl;

    // 状态
    private String status;

    // 同步信息
    private Boolean syncedToShopify;
    private String syncError;
    private LocalDateTime syncedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
