package com.logistics.track17.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 钉钉登录请求DTO
 */
@Data
public class DingTalkLoginRequest {

    /**
     * 授权码（钉钉回调返回的code）
     */
    @NotBlank(message = "授权码不能为空")
    private String code;

    /**
     * CSRF防护token
     */
    private String state;
}
