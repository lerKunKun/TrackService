package com.logistics.track17.dto;

import lombok.Data;

/**
 * 产品模板显示DTO
 */
@Data
public class ProductTemplateDTO {
    private Long id; // product_template ID
    private Long productId; // 产品ID
    private String productName; // 产品名称 (Product.title)
    private String templateName; // 模板名称 (可编辑)
    private String templateVersion;// 模板版本
    private String storeIdentifier;// 店铺 (可编辑)
}
