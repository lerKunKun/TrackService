package com.logistics.track17.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品变体实体类 (基于 Shopify Variant 模型)
 * P1: 基础变体信息 (价格、图片)
 * P2: 添加变体选项属性 (option1-3) 和 SKU、采购链接
 */
@Data
public class ProductVariant {
    /**
     * 变体ID
     */
    private Long id;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 变体标题
     */
    private String title;

    /**
     * 销售价格
     */
    private BigDecimal price;

    /**
     * 原价 (对比价格)
     */
    private BigDecimal compareAtPrice;

    /**
     * 变体图片URL
     */
    private String imageUrl;

    /**
     * 库存数量
     */
    private Integer inventoryQuantity;

    /**
     * 重量 (克)
     */
    private BigDecimal weight;

    /**
     * 条形码
     */
    private String barcode;

    // ============ P2 采购扩展字段 ============

    /**
     * 选项1名称 (如: Color)
     */
    private String option1Name;

    /**
     * 选项1值 (如: Red)
     */
    private String option1Value;

    /**
     * 选项2名称 (如: Size)
     */
    private String option2Name;

    /**
     * 选项2值 (如: Large)
     */
    private String option2Value;

    /**
     * 选项3名称 (如: Material)
     */
    private String option3Name;

    /**
     * 选项3值 (如: Cotton)
     */
    private String option3Value;

    /**
     * SKU库存单位
     */
    private String sku;

    /**
     * 采购链接
     */
    private String procurementUrl;

    /**
     * 采购价格
     */
    private BigDecimal procurementPrice;

    /**
     * 采购商名称
     */
    private String supplier;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
