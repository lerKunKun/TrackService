package com.logistics.track17.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 店铺实体
 */
@Data
public class Shop {
    private Long id;
    private String shopName;
    private String platform;
    private String storeUrl;
    private String apiKey;
    private String apiSecret;
    private String accessToken;
    private String timezone;
    private String shopDomain;      // Shopify店铺域名 (xxx.myshopify.com)
    private String oauthState;      // OAuth state nonce (安全验证)
    private String oauthScope;      // OAuth授权的scope
    private LocalDateTime tokenExpiresAt;  // Token过期时间
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
