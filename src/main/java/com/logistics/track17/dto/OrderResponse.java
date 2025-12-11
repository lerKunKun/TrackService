package com.logistics.track17.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应DTO
 */
@Data
public class OrderResponse {
    private Long id;
    private Long shopId;
    private String shopName;
    private Long shopifyOrderId;
    private String orderNumber;
    private String orderName;

    // 客户信息
    private String customerEmail;
    private String customerName;
    private String customerPhone;

    // 金额
    private BigDecimal totalPrice;
    private String currency;

    // 状态
    private String financialStatus;
    private String fulfillmentStatus;

    // 收货地址
    private String shippingAddressName;
    private String shippingAddressCity;
    private String shippingAddressCountry;

    // 时间
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
