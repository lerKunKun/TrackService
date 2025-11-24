package com.logistics.track17.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 承运商实体
 */
@Data
public class Carrier {
    private Long id;
    private Integer carrierId;        // 17Track承运商ID
    private String carrierCode;       // 系统承运商代码
    private String carrierName;       // 承运商英文名称
    private String carrierNameCn;     // 承运商中文名称
    private Integer countryId;        // 国家ID
    private String countryIso;        // 国家ISO代码
    private String email;             // 联系邮箱
    private String tel;               // 联系电话
    private String url;               // 官网地址
    private Boolean isActive;         // 是否启用
    private Integer sortOrder;        // 排序
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
