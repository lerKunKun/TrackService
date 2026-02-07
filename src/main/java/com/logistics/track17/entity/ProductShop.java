package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 产品商店关联实体类 (多对多中间表)
 * 一个产品可以上架到多个商店
 */
@Data
public class ProductShop {
    /**
     * 关联ID
     */
    private Long id;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 商店ID
     */
    private Long shopId;

    /**
     * Who published this product (user ID)
     */
    private Long publishedBy;

    /**
     * Publish status (0: not published, 1: published, 2: unpublished)
     */
    private Integer publishStatus;

    /**
     * Last export time
     */
    private LocalDateTime lastExportTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
