package com.logistics.track17.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductTemplateDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String templateName;
    private String templateVersion;
    private String storeIdentifier;
    private Long sourceShopId;
    private String sourceShopName;
    private LocalDateTime lastPullTime;
    private LocalDateTime lastPushTime;
}
