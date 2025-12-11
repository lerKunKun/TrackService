package com.logistics.track17.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 店铺响应DTO
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopResponse {
    private Long id;
    private String shopName;
    private String platform;
    private String storeUrl;
    private String shopDomain; // Shopify店铺域名
    private String apiKey; // 不返回完整的密钥,只返回前4位
    private String timezone;
    private Long orderCount;
    private String connectionStatus; // 连接状态
    private LocalDateTime lastValidatedAt; // 最后验证时间

    // Shopify商店详细信息
    private String contactEmail; // 联系邮箱
    private String ownerEmail; // 店主邮箱
    private String currency; // 货币代码
    private String planName; // 计划名称
    private String planDisplayName; // 计划显示名称
    private Boolean isShopifyPlus; // 是否Plus
    private String primaryDomain; // 主域名
    private String shopOwner; // 店主姓名
    private String ianaTimezone; // IANA时区
    private java.util.Map<String, Object> shopInfoDetails; // 解析的完整商店信息

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
