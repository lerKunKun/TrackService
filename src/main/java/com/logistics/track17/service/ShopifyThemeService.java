package com.logistics.track17.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.track17.entity.Shop;
import com.logistics.track17.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Shopify Theme API 客户端，用于拉取/推送主题资产文件。
 * 所有请求通过 shop 的 accessToken 鉴权。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopifyThemeService {

    private static final String API_VERSION = "2024-10";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 获取店铺已发布（role=main）主题的 ID
     */
    public Long getPublishedThemeId(Shop shop) {
        String url = buildUrl(shop, "/themes.json");
        HttpEntity<String> request = new HttpEntity<>(buildHeaders(shop));

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode themes = objectMapper.readTree(response.getBody()).get("themes");
            if (themes != null && themes.isArray()) {
                for (JsonNode theme : themes) {
                    if ("main".equals(theme.get("role").asText())) {
                        return theme.get("id").asLong();
                    }
                }
            }
            throw BusinessException.of("未找到已发布的主题 (role=main)，店铺: " + shop.getShopDomain());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取主题列表失败, shop={}", shop.getShopDomain(), e);
            throw BusinessException.of("获取主题列表失败: " + e.getMessage());
        }
    }

    /**
     * 读取主题资产文件内容
     *
     * @param assetKey 例如 "config/settings_data.json"
     * @return 文件内容字符串
     */
    public String getAsset(Shop shop, Long themeId, String assetKey) {
        String url = buildUrl(shop, "/themes/" + themeId + "/assets.json?asset[key]=" + assetKey);
        HttpEntity<String> request = new HttpEntity<>(buildHeaders(shop));

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode asset = objectMapper.readTree(response.getBody()).get("asset");
            if (asset != null && asset.has("value")) {
                return asset.get("value").asText();
            }
            throw BusinessException.of("资产文件为空: " + assetKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("拉取资产失败, shop={}, themeId={}, key={}", shop.getShopDomain(), themeId, assetKey, e);
            throw BusinessException.of("拉取主题资产失败 [" + assetKey + "]: " + e.getMessage());
        }
    }

    /**
     * 写入（创建或更新）主题资产文件
     */
    public void putAsset(Shop shop, Long themeId, String assetKey, String content) {
        String url = buildUrl(shop, "/themes/" + themeId + "/assets.json");

        Map<String, Object> assetBody = new HashMap<>();
        assetBody.put("key", assetKey);
        assetBody.put("value", content);

        Map<String, Object> body = new HashMap<>();
        body.put("asset", assetBody);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders(shop));

        try {
            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            log.info("推送资产成功, shop={}, themeId={}, key={}", shop.getShopDomain(), themeId, assetKey);
        } catch (Exception e) {
            log.error("推送资产失败, shop={}, themeId={}, key={}", shop.getShopDomain(), themeId, assetKey, e);
            throw BusinessException.of("推送主题资产失败 [" + assetKey + "]: " + e.getMessage());
        }
    }

    private HttpHeaders buildHeaders(Shop shop) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", shop.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String buildUrl(Shop shop, String path) {
        return String.format("https://%s/admin/api/%s%s", shop.getShopDomain(), API_VERSION, path);
    }
}
