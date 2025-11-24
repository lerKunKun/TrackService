package com.logistics.track17.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物流事件响应DTO
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackingEventResponse {
    private Long id;
    private LocalDateTime eventTime;
    private String description;         // 事件描述
    private String location;            // 事件位置
    private String city;                // 城市
    private String postalCode;          // 邮编
    private String stage;               // 阶段: InfoReceived/InTransit/Delivered等
    private String subStatus;           // 子状态
    private String providerName;        // 承运商名称
    private String timeIso;             // ISO时间字符串

    // 兼容旧字段名
    private String status;              // 等同于description
}
