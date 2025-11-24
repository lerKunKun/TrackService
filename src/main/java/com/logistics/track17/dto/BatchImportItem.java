package com.logistics.track17.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 批量导入运单项
 */
@Data
public class BatchImportItem {

    @NotBlank(message = "运单号不能为空")
    private String trackingNumber;

    private String carrierCode;  // 承运商代码（可选，为空时自动识别）

    private String remarks;
}
