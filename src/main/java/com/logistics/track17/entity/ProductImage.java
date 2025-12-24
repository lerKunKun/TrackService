package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 产品图片实体类
 * MVP阶段可选，主要使用 ProductVariant.imageUrl
 */
@Data
public class ProductImage {
    /**
     * 图片ID
     */
    private Long id;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 关联的变体ID (可选)
     */
    private Long variantId;

    /**
     * 图片URL
     */
    private String src;

    /**
     * 替代文本
     */
    private String altText;

    /**
     * 排序位置
     */
    private Integer position;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
