package com.logistics.track17.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Shopify订单实体
 */
@Data
public class Order {
    private Long id;
    private Long shopId;
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
    private String shippingAddressAddress1;
    private String shippingAddressCity;
    private String shippingAddressProvince;
    private String shippingAddressCountry;
    private String shippingAddressZip;
    private String shippingAddressPhone;

    // 时间
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime syncedAt;

    // 原始数据
    private String rawData;
}
