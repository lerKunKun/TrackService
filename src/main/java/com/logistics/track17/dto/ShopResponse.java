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
    private String apiKey;  // 不返回完整的密钥，只返回前4位
    private String timezone;
    private Long orderCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
