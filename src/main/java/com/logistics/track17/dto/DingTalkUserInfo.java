package com.logistics.track17.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 钉钉用户信息DTO
 */
@Data
public class DingTalkUserInfo {

    /**
     * 钉钉unionId（跨企业唯一标识）
     */
    @JsonProperty("unionId")
    private String unionId;

    /**
     * openId
     */
    @JsonProperty("openId")
    private String openId;

    /**
     * 用户昵称
     */
    @JsonProperty("nick")
    private String nick;

    /**
     * 头像URL
     */
    @JsonProperty("avatarUrl")
    private String avatarUrl;

    /**
     * 手机号
     */
    @JsonProperty("mobile")
    private String mobile;

    /**
     * 邮箱
     */
    @JsonProperty("email")
    private String email;

    /**
     * 国家码
     */
    @JsonProperty("stateCode")
    private String stateCode;
}
