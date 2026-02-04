package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 产品主体实体类 (基于 Shopify Product 模型)
 * P1: 产品基础信息
 * P3: 添加产品刊登链接
 */
@Data
public class Product {
    /**
     * 产品ID
     */
    private Long id;

    /**
     * 产品唯一标识符 (用于URL)
     */
    private String handle;

    /**
     * 产品标题
     */
    private String title;

    /**
     * 产品描述 (HTML格式)
     */
    private String bodyHtml;

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
     * 产品刊登链接 (P3使用)
     */
    private String productUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 刊登状态 (非数据库字段, 聚合自 product_shops)
     */
    private Integer publishStatus;

    /**
     * 最后导出时间 (非数据库字段, 聚合自 product_shops)
     */
    private LocalDateTime lastExportTime;

    /**
     * 产品主图URL (非数据库字段, 取自第一个变体)
     */
    private String imageUrl;
}
