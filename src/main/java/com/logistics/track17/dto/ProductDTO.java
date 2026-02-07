package com.logistics.track17.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 产品DTO (包含产品和第一个变体信息)
 * 用于列表展示
 */
@Data
public class ProductDTO {
    /**
     * 产品ID
     */
    private Long id;

    /**
     * 产品唯一标识
     */
    private String handle;

    /**
     * 产品标题
     */
    private String title;

    /**
     * 品牌/制造商
     */
    private String vendor;

    /**
     * 标签 (逗号分隔)
     */
    private String tags;

    /**
     * 上架状态: 0-草稿, 1-已上架
     */
    private Integer published;

    /**
     * 关联的商店ID列表
     */
    private List<Long> shopIds;

    /**
     * 第一个变体的图片URL
     */
    private String imageUrl;

    /**
     * 第一个变体的价格
     */
    private BigDecimal price;

    /**
     * 第一个变体的原价
     */
    private BigDecimal compareAtPrice;

    /**
     * 变体数量
     */
    private Integer variantCount;
}
