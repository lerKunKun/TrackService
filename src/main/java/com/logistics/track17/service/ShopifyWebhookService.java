package com.logistics.track17.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.track17.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Shopify Webhook服务
 * 参考: https://shopify.dev/docs/apps/build/webhooks
 */
@Slf4j
@Service
public class ShopifyWebhookService {

    @Value("${shopify.api.secret:}")
    private String shopifyApiSecret;

    @Value("${shopify.webhook.base-url:http://localhost:8080/api/v1}")
    private String webhookBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ShopifyWebhookService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        log.info("=== Shopify Webhook Service Initialized ===");
        log.info("Webhook Base URL: {}", webhookBaseUrl);
        log.info("API Secret configured: {}", shopifyApiSecret != null && !shopifyApiSecret.isEmpty());
    }

    /**
     * 注册所有必要的webhooks
     *
     * @param shopDomain  店铺域名
     * @param accessToken 访问令牌
     * @return 注册结果
     */
    public Map<String, Object> registerAllWebhooks(String shopDomain, String accessToken) {
        log.info("Registering webhooks for shop: {}", shopDomain);

        Map<String, Object> result = new HashMap<>();
        List<String> successTopics = new ArrayList<>();
        List<String> failedTopics = new ArrayList<>();

        // 定义需要注册的webhook主题
        Map<String, String> webhookTopics = new HashMap<>();
        webhookTopics.put("shop/update", webhookBaseUrl + "/webhooks/shopify/shop-update");
        webhookTopics.put("app/uninstalled", webhookBaseUrl + "/webhooks/shopify/app-uninstalled");
        webhookTopics.put("orders/create", webhookBaseUrl + "/webhooks/shopify/orders-create");
        webhookTopics.put("orders/updated", webhookBaseUrl + "/webhooks/shopify/orders-updated");
        // 支付争议 webhook
        webhookTopics.put("disputes/create", webhookBaseUrl + "/webhooks/shopify/disputes-create");
        webhookTopics.put("disputes/update", webhookBaseUrl + "/webhooks/shopify/disputes-update");

        // 注册每个webhook
        for (Map.Entry<String, String> entry : webhookTopics.entrySet()) {
            String topic = entry.getKey();
            String address = entry.getValue();

            try {
                boolean registered = registerWebhook(shopDomain, accessToken, topic, address);
                if (registered) {
                    successTopics.add(topic);
                } else {
                    failedTopics.add(topic);
                }
            } catch (Exception e) {
                log.error("Failed to register webhook: {} for shop: {}", topic, shopDomain, e);
                failedTopics.add(topic);
            }
        }

        result.put("shop", shopDomain);
        result.put("success", successTopics);
        result.put("failed", failedTopics);
        result.put("totalSuccess", successTopics.size());
        result.put("totalFailed", failedTopics.size());

        log.info("Webhook registration completed for {}: {} success, {} failed",
                shopDomain, successTopics.size(), failedTopics.size());

        return result;
    }

    /**
     * 注册单个webhook
     *
     * @param shopDomain  店铺域名
     * @param accessToken 访问令牌
     * @param topic       webhook主题
     * @param address     webhook回调地址
     * @return 是否成功
     */
    public boolean registerWebhook(String shopDomain, String accessToken, String topic, String address) {
        log.info("Registering webhook: {} for shop: {}", topic, shopDomain);

        try {
            // 先检查是否已存在
            List<Map<String, Object>> existingWebhooks = getWebhooks(shopDomain, accessToken);
            for (Map<String, Object> webhook : existingWebhooks) {
                if (topic.equals(webhook.get("topic"))) {
                    String existingAddress = (String) webhook.get("address");
                    if (address.equals(existingAddress)) {
                        log.info("Webhook already exists with same URL: {} for shop: {}", topic, shopDomain);
                        return true;
                    } else {
                        // 同topic但不同address，删除旧的
                        Long webhookId = (Long) webhook.get("id");
                        log.info("Deleting old webhook {} (topic: {}, old URL: {}) to register new URL: {}",
                                webhookId, topic, existingAddress, address);
                        deleteWebhook(shopDomain, accessToken, webhookId);
                    }
                }
            }

            String webhookUrl = String.format("https://%s/admin/api/2024-10/webhooks.json", shopDomain);

            // 构建请求body
            Map<String, Object> webhookData = new HashMap<>();
            webhookData.put("topic", topic);
            webhookData.put("address", address);
            webhookData.put("format", "json");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("webhook", webhookData);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Shopify-Access-Token", accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 调用Shopify API
            ResponseEntity<String> response = restTemplate.postForEntity(
                    webhookUrl,
                    request,
                    String.class);

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully registered webhook: {} for shop: {}", topic, shopDomain);
                return true;
            } else {
                log.error("Failed to register webhook: {}, status: {}", topic, response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("Error registering webhook: {} for shop: {}", topic, shopDomain, e);
            return false;
        }
    }

    /**
     * 获取已注册的webhooks
     *
     * @param shopDomain  店铺域名
     * @param accessToken 访问令牌
     * @return webhook列表
     */
    public List<Map<String, Object>> getWebhooks(String shopDomain, String accessToken) {
        try {
            String webhookUrl = String.format("https://%s/admin/api/2024-10/webhooks.json", shopDomain);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Shopify-Access-Token", accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.GET,
                    request,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                JsonNode webhooksNode = responseJson.get("webhooks");

                List<Map<String, Object>> webhooks = new ArrayList<>();
                if (webhooksNode != null && webhooksNode.isArray()) {
                    for (JsonNode webhookNode : webhooksNode) {
                        Map<String, Object> webhook = new HashMap<>();
                        webhook.put("id", webhookNode.get("id").asLong());
                        webhook.put("topic", webhookNode.get("topic").asText());
                        webhook.put("address", webhookNode.get("address").asText());
                        webhook.put("format", webhookNode.get("format").asText());
                        webhooks.add(webhook);
                    }
                }

                return webhooks;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Error fetching webhooks for shop: {}", shopDomain, e);
            return new ArrayList<>();
        }
    }

    /**
     * 验证webhook请求签名
     * 参考:
     * https://shopify.dev/docs/apps/build/webhooks/subscribe/https#step-5-verify-the-webhook
     *
     * @param requestBody webhook请求体(原始字符串)
     * @param hmacHeader  X-Shopify-Hmac-SHA256头
     * @return 是否有效
     */
    public boolean verifyWebhookSignature(String requestBody, String hmacHeader) {
        if (hmacHeader == null || hmacHeader.isEmpty()) {
            log.warn("Webhook HMAC header missing");
            return false;
        }

        try {
            // 使用API Secret计算HMAC-SHA256
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    shopifyApiSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));
            String calculatedHmac = Base64.getEncoder().encodeToString(hash);

            boolean valid = calculatedHmac.equals(hmacHeader);
            if (!valid) {
                log.warn("Webhook HMAC validation failed. Expected: {}, Got: {}", calculatedHmac, hmacHeader);
            }

            return valid;

        } catch (Exception e) {
            log.error("Error validating webhook HMAC", e);
            return false;
        }
    }

    /**
     * 删除webhook
     *
     * @param shopDomain  店铺域名
     * @param accessToken 访问令牌
     * @param webhookId   webhook ID
     * @return 是否成功
     */
    public boolean deleteWebhook(String shopDomain, String accessToken, Long webhookId) {
        log.info("Deleting webhook: {} for shop: {}", webhookId, shopDomain);

        try {
            String webhookUrl = String.format("https://%s/admin/api/2024-10/webhooks/%d.json",
                    shopDomain, webhookId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Shopify-Access-Token", accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.DELETE,
                    request,
                    String.class);

            boolean success = response.getStatusCode() == HttpStatus.OK ||
                    response.getStatusCode() == HttpStatus.NO_CONTENT;

            if (success) {
                log.info("Successfully deleted webhook: {} for shop: {}", webhookId, shopDomain);
            } else {
                log.error("Failed to delete webhook: {}, status: {}", webhookId, response.getStatusCode());
            }

            return success;

        } catch (Exception e) {
            log.error("Error deleting webhook: {} for shop: {}", webhookId, shopDomain, e);
            return false;
        }
    }

    /**
     * 删除所有webhooks
     *
     * @param shopDomain  店铺域名
     * @param accessToken 访问令牌
     * @return 删除数量
     */
    public int deleteAllWebhooks(String shopDomain, String accessToken) {
        log.info("Deleting all webhooks for shop: {}", shopDomain);

        List<Map<String, Object>> webhooks = getWebhooks(shopDomain, accessToken);
        int deletedCount = 0;

        for (Map<String, Object> webhook : webhooks) {
            Long webhookId = (Long) webhook.get("id");
            if (deleteWebhook(shopDomain, accessToken, webhookId)) {
                deletedCount++;
            }
        }

        log.info("Deleted {} webhooks for shop: {}", deletedCount, shopDomain);
        return deletedCount;
    }
}
