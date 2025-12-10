package com.logistics.track17.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 钉钉配置类
 */
@Configuration
@ConfigurationProperties(prefix = "dingtalk")
@Data
public class DingTalkConfig {

    /**
     * 企业CorpId
     */
    private String corpId;

    /**
     * AppKey (ClientId)
     */
    private String appKey;

    /**
     * AppSecret (ClientSecret)
     */
    private String appSecret;

    /**
     * 回调URI
     */
    private String redirectUri;

    /**
     * 扫码登录页面URL
     */
    private String qrConnectUrl;

    /**
     * OAuth Token获取URL
     */
    private String tokenUrl;

    /**
     * 获取用户信息URL
     */
    private String userInfoUrl;

    /**
     * 生成钉钉登录URL
     *
     * @param state CSRF防护token
     * @return 钉钉登录URL
     */
    public String getLoginUrl(String state) {
        try {
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
            return qrConnectUrl +
                    "?appid=" + appKey +
                    "&response_type=code" +
                    "&scope=snsapi_login" +
                    "&state=" + state +
                    "&redirect_uri=" + encodedRedirectUri;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to encode redirect URI", e);
        }
    }
}
