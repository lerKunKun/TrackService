package com.logistics.track17.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包裹实体
 */
@Data
public class Parcel {
    private Long id;
    private Long orderId;
    private String parcelNo;
    private String carrierCode;
    private String carrierName;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private String status;  // in_transit / delivered / exception
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
