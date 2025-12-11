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
     * 生成钉钉登录URL (OAuth 2.0)
     *
     * @param state CSRF防护token
     * @return 钉钉登录URL
     */
    public String getLoginUrl(String state) {
        try {
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
            // OAuth 2.0 参数：使用client_id替代appid，scope=openid，添加iframe=true支持嵌入式登录
            return qrConnectUrl +
                    "?client_id=" + appKey +
                    "&response_type=code" +
                    "&scope=openid" +
                    "&state=" + state +
                    "&redirect_uri=" + encodedRedirectUri +
                    "&prompt=consent" +
                    "&iframe=true"; // 关键：支持iframe嵌入式登录
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to encode redirect URI", e);
        }
    }
}
