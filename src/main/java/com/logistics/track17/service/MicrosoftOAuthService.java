package com.logistics.track17.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.track17.entity.EmailMonitorConfig;
import com.logistics.track17.mapper.EmailMonitorConfigMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Microsoft OAuth2 Modern Authentication 服务
 * 用于 Outlook/Hotmail 邮箱的 IMAP 授权
 */
@Slf4j
@Service
public class MicrosoftOAuthService {

    private static final String AUTH_ENDPOINT =
            "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    private static final String TOKEN_ENDPOINT =
            "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    // IMAP 访问权限 + 离线访问（用于 refresh_token）
    private static final String SCOPE =
            "https://outlook.office.com/IMAP.AccessAsUser.All offline_access";

    @Value("${microsoft.oauth2.client-id:}")
    private String clientId;

    @Value("${microsoft.oauth2.client-secret:}")
    private String clientSecret;

    @Value("${microsoft.oauth2.redirect-uri:}")
    private String redirectUri;

    private final EmailMonitorConfigMapper configMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    // state -> configId 临时映射（10分钟超时，防CSRF）
    private final Map<String, StateEntry> stateMap = new ConcurrentHashMap<>();

    public MicrosoftOAuthService(EmailMonitorConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    /**
     * 检查是否已配置 Microsoft OAuth2
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty();
    }

    /**
     * 生成 Microsoft OAuth2 授权 URL
     */
    public String buildAuthUrl(Long configId) {
        String state = UUID.randomUUID().toString().replace("-", "");
        stateMap.put(state, new StateEntry(configId, System.currentTimeMillis()));
        cleanExpiredStates();

        return AUTH_ENDPOINT
                + "?client_id=" + encode(clientId)
                + "&response_type=code"
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode(SCOPE)
                + "&state=" + state
                + "&response_mode=query";
    }

    /**
     * 从 state 中取出 configId（一次性，取后删除）
     */
    public Long getConfigIdFromState(String state) {
        StateEntry entry = stateMap.remove(state);
        if (entry == null) return null;
        // 10分钟内有效
        if (System.currentTimeMillis() - entry.createdAt > 10 * 60 * 1000L) return null;
        return entry.configId;
    }

    /**
     * 用授权码换取 access_token + refresh_token，保存到 DB
     */
    public void exchangeCodeAndSave(Long configId, String code) {
        RequestBody body = new FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .add("grant_type", "authorization_code")
                .build();
        TokenResult result = postTokenRequest(body);
        saveTokens(configId, result);
        log.info("Microsoft OAuth2 授权成功: configId={}", configId);
    }

    /**
     * 刷新 access_token，更新 DB，返回新 token
     */
    public String refreshAccessToken(EmailMonitorConfig config) {
        if (config.getRefreshToken() == null || config.getRefreshToken().isEmpty()) {
            throw new RuntimeException("refresh_token 不存在，请重新授权");
        }
        RequestBody body = new FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("refresh_token", config.getRefreshToken())
                .add("grant_type", "refresh_token")
                .add("scope", SCOPE)
                .build();
        TokenResult result = postTokenRequest(body);
        saveTokens(config.getId(), result);
        log.info("Microsoft access_token 已刷新: configId={}", config.getId());
        return result.accessToken;
    }

    /**
     * 获取有效的 access_token（自动刷新）
     */
    public String getValidAccessToken(EmailMonitorConfig config) {
        // 提前5分钟刷新
        if (config.getTokenExpiry() != null
                && LocalDateTime.now().isBefore(config.getTokenExpiry().minusMinutes(5))
                && config.getAccessToken() != null) {
            return config.getAccessToken();
        }
        return refreshAccessToken(config);
    }

    // ==================== 私有方法 ====================

    private TokenResult postTokenRequest(RequestBody body) {
        Request request = new Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(body)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            String json = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Microsoft token 请求失败: status={}, body={}", response.code(), json);
                throw new RuntimeException("获取 token 失败: " + json);
            }
            JsonNode node = objectMapper.readTree(json);
            String accessToken = node.path("access_token").asText();
            String refreshToken = node.path("refresh_token").asText(null);
            int expiresIn = node.path("expires_in").asInt(3600);
            if (accessToken.isEmpty()) {
                throw new RuntimeException("token 响应中没有 access_token: " + json);
            }
            return new TokenResult(accessToken, refreshToken, expiresIn);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("token 请求异常: " + e.getMessage(), e);
        }
    }

    private void saveTokens(Long configId, TokenResult result) {
        LocalDateTime expiry = LocalDateTime.now().plusSeconds(result.expiresIn);
        configMapper.updateTokens(configId, result.accessToken,
                result.refreshToken, expiry);
    }

    private void cleanExpiredStates() {
        long now = System.currentTimeMillis();
        stateMap.entrySet().removeIf(e -> now - e.getValue().createdAt > 10 * 60 * 1000L);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // ==================== 内部类 ====================

    private static class StateEntry {
        final Long configId;
        final long createdAt;

        StateEntry(Long configId, long createdAt) {
            this.configId = configId;
            this.createdAt = createdAt;
        }
    }

    private static class TokenResult {
        final String accessToken;
        final String refreshToken;
        final int expiresIn;

        TokenResult(String accessToken, String refreshToken, int expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
    }
}
