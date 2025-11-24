package com.logistics.track17.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.track17.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Shopify OAuth服务
 * 参考: https://shopify.dev/docs/apps/build/authentication-authorization/access-tokens/authorization-code-grant
 */
@Slf4j
@Service
public class ShopifyOAuthService {

    @Value("${shopify.api.key:}")
    private String shopifyApiKey;

    @Value("${shopify.api.secret:}")
    private String shopifyApiSecret;

    @Value("${shopify.oauth.redirect-uri:http://localhost:8080/api/v1/oauth/shopify/callback}")
    private String redirectUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ShopifyOAuthService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成OAuth授权URL
     *
     * @param shopDomain Shopify店铺域名 (例如: mystore.myshopify.com)
     * @return 授权URL
     */
    public String generateAuthorizationUrl(String shopDomain) {
        log.info("Generating OAuth authorization URL for shop: {}", shopDomain);

        // 生成随机state用于防止CSRF攻击
        String state = generateState();

        // Shopify推荐的scopes（根据需求调整）
        // 只请求基本的读取权限
        String scopes = "read_orders";

        // 构建OAuth授权URL
        String authUrl = UriComponentsBuilder
                .fromHttpUrl(String.format("https://%s/admin/oauth/authorize", shopDomain))
                .queryParam("client_id", shopifyApiKey)
                .queryParam("scope", scopes)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build()
                .toUriString();

        log.info("Authorization URL generated: {}", authUrl);
        return authUrl;
    }

    /**
     * 使用授权码换取访问令牌
     *
     * @param code 授权码
     * @param shopDomain Shopify店铺域名
     * @return 访问令牌信息（包含access_token和scope）
     */
    public Map<String, Object> exchangeCodeForToken(String code, String shopDomain) {
        log.info("Exchanging authorization code for access token, shop: {}", shopDomain);

        String tokenUrl = String.format("https://%s/admin/oauth/access_token", shopDomain);

        // 构建请求body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("client_id", shopifyApiKey);
        requestBody.put("client_secret", shopifyApiSecret);
        requestBody.put("code", code);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            // 调用Shopify API
            ResponseEntity<String> response = restTemplate.postForEntity(
                    tokenUrl,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // 解析响应
                JsonNode responseJson = objectMapper.readTree(response.getBody());

                Map<String, Object> result = new HashMap<>();
                result.put("access_token", responseJson.get("access_token").asText());
                result.put("scope", responseJson.get("scope").asText());

                log.info("Successfully obtained access token for shop: {}", shopDomain);
                return result;
            } else {
                log.error("Failed to exchange code for token: {}", response.getStatusCode());
                throw BusinessException.of("获取访问令牌失败");
            }

        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            throw BusinessException.of("OAuth授权失败: " + e.getMessage());
        }
    }

    /**
     * 验证HMAC签名（Shopify回调参数验证）
     *
     * @param params 回调参数
     * @return 是否有效
     */
    public boolean validateHmac(Map<String, String> params) {
        String hmac = params.get("hmac");
        if (hmac == null) {
            log.warn("HMAC parameter missing");
            return false;
        }

        try {
            // 移除hmac参数
            Map<String, String> paramsToSign = new HashMap<>(params);
            paramsToSign.remove("hmac");

            // 按key排序并构建查询字符串
            String queryString = paramsToSign.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));

            // 计算HMAC-SHA256
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    shopifyApiSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(queryString.getBytes(StandardCharsets.UTF_8));
            String calculatedHmac = bytesToHex(hash);

            boolean valid = calculatedHmac.equalsIgnoreCase(hmac);
            if (!valid) {
                log.warn("HMAC validation failed. Expected: {}, Got: {}", calculatedHmac, hmac);
            }

            return valid;

        } catch (Exception e) {
            log.error("Error validating HMAC", e);
            return false;
        }
    }

    /**
     * 生成随机state
     */
    private String generateState() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 验证店铺域名格式
     */
    public boolean isValidShopDomain(String shopDomain) {
        if (shopDomain == null || shopDomain.isEmpty()) {
            return false;
        }
        // 验证格式: xxx.myshopify.com
        return shopDomain.matches("^[a-zA-Z0-9-]+\\.myshopify\\.com$");
    }
}
