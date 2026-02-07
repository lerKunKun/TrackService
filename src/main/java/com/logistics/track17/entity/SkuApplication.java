package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * SKU申请记录实体类 (P2使用)
 * 记录产品变体SKU申请和审核
 */
@Data
public class SkuApplication {
    /**
     * 申请ID
     */
    private Long id;

    /**
     * 产品变体ID
     */
    private Long productVariantId;

    /**
     * 申请状态: 0-待审核, 1-已通过, 2-已拒绝
     */
    private Integer status;

    /**
     * 生成的SKU
     */
    private String generatedSku;

    /**
     * 申请时间
     */
    private LocalDateTime appliedAt;

    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;
}
