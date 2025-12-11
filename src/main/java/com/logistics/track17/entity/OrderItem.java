package com.logistics.track17.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单商品实体
 */
@Data
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long shopifyLineItemId;

    // 商品信息
    private String sku;
    private String title;
    private String variantTitle;

    // 数量和价格
    private Integer quantity;
    private BigDecimal price;

    private LocalDateTime createdAt;
}
