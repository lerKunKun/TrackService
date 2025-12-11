package com.logistics.track17.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 店铺实体
 */
@Data
public class Shop {
    private Long id;
    private Long userId;
    private String shopName;
    private String platform;
    private String storeUrl;
    private String shopDomain; // Shopify店铺域名 (xxx.myshopify.com)
    private String timezone; // 店铺时区
    private String apiKey;
    private String apiSecret;
    private String accessToken;
    private String tokenType; // Token类型：offline(永久), online(24小时)
    private String connectionStatus; // 连接状态：active(正常), invalid(失效), pending(待授权)
    private LocalDateTime lastValidatedAt; // 最后验证时间
    private String webhookSecret;
    private String oauthState; // OAuth state nonce (安全验证)
    private String oauthScope; // OAuth授权的scope
    private LocalDateTime tokenExpiresAt; // Token过期时间（online token使用）

    // Shopify商店详细信息字段
    private String contactEmail; // 商店联系邮箱
    private String ownerEmail; // 店主邮箱
    private String currency; // 商店货币代码
    private String planName; // 订阅计划名称
    private String planDisplayName; // 计划显示名称
    private Boolean isShopifyPlus; // 是否为Shopify Plus
    private String primaryDomain; // 主域名
    private String shopOwner; // 店主姓名
    private String ianaTimezone; // IANA时区标识
    private String shopInfoJson; // 完整商店信息JSON（用于扩展存储）

    private Boolean isActive; // 是否激活
    private LocalDateTime lastSyncTime; // 最后同步时间
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt; // 删除时间（软删除）
}
