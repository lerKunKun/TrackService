package com.logistics.track17.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 店铺请求DTO
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopRequest {

    @NotBlank(message = "店铺名称不能为空")
    private String shopName;

    @NotBlank(message = "平台类型不能为空")
    private String platform;  // shopify / shopline / tiktokshop

    private String storeUrl;

    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    @NotBlank(message = "API Secret不能为空")
    private String apiSecret;

    @NotBlank(message = "Access Token不能为空")
    private String accessToken;

    private String timezone;
}
