package com.logistics.track17.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 采购管理列表DTO - 仅包含摘要信息以提升性能
 */
@Data
public class ProductProcurementSummaryDTO {
    private Long id;
    private String title;
    private String imageUrl;
    private Integer variantCount; // 总变体数
    private Integer completeVariantCount; // 已完善变体数
    private String publishStatus;
    private LocalDateTime lastExportTime;
}
