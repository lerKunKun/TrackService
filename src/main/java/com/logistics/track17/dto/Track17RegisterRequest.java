package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 17Track注册请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Track17RegisterRequest {
    private String number;  // 运单号
    private Integer carrier;  // 承运商代码（17Track的carrier ID）
    private Map<String, String> param;  // 额外参数，如org（始发国）, dst（目的国）

    public Track17RegisterRequest(String number, String carrierCode) {
        this.number = number;
        this.carrier = convertCarrierCode(carrierCode);
    }

    /**
     * 转换承运商代码为17Track的carrier ID
     * carrier = 0 时，17Track会自动识别承运商
     * 系统统一使用自动识别模式
     */
    private Integer convertCarrierCode(String carrierCode) {
        // 统一使用自动识别
        return 0;
    }
}
