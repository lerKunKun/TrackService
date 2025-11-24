package com.logistics.track17.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 运单添加请求
 */
@Data
public class TrackingRequest {

    @NotBlank(message = "运单号不能为空")
    private String trackingNumber;

    private String carrierCode;  // 承运商代码（可选，为空时自动识别）

    private Long orderId;  // 关联订单ID（可选）

    private String source;  // 来源：manual / shopify / api

    private String remarks;  // 备注
}
