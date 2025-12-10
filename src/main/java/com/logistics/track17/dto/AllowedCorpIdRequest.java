package com.logistics.track17.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 允许CorpId请求DTO
 */
@Data
public class AllowedCorpIdRequest {

    /**
     * 企业CorpId
     */
    @NotBlank(message = "企业CorpId不能为空")
    private String corpId;

    /**
     * 企业名称
     */
    private String corpName;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
}
