package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 产品模板快照实体
 */
@Data
public class ProductTemplateSuffix {

    private Long id;

    /** 关联的迁移ID */
    private Long migrationId;

    /** Shopify产品ID */
    private String shopifyProductId;

    /** 产品标题 */
    private String productTitle;

    /** 模板后缀 */
    private String templateSuffix;

    /** 快照时间 */
    private LocalDateTime snapshotDate;
}
