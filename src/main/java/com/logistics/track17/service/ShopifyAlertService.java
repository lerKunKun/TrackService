package com.logistics.track17.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.track17.entity.Shop;
import com.logistics.track17.enums.AlertType;
import com.logistics.track17.mapper.ShopMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

/**
 * Shopify告警API轮询服务
 * 定时通过GraphQL API查询shop.alerts和争议信息
 */
@Service
@Slf4j
public class ShopifyAlertService {

    private final ShopMapper shopMapper;
    private final DingtalkNotificationService notificationService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${shopify.api.secret:}")
    private String shopifyApiSecret;

    public ShopifyAlertService(ShopMapper shopMapper,
            DingtalkNotificationService notificationService,
            ObjectMapper objectMapper) {
        this.shopMapper = shopMapper;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 定时轮询Shop Alerts (每30分钟)
     */
    @Scheduled(fixedDelayString = "${shopify.alert.poll.interval:1800000}", initialDelay = 120000)
    public void pollShopAlerts() {
        List<Shop> activeShops = shopMapper.findByIsActive(true);
        if (activeShops.isEmpty()) {
            return;
        }

        log.debug("开始轮询Shopify Shop Alerts, 共{}个活跃店铺", activeShops.size());

        for (Shop shop : activeShops) {
            try {
                checkShopAlerts(shop);
            } catch (Exception e) {
                log.error("轮询店铺告警失败: shop={}", shop.getShopDomain(), e);
            }
        }
    }

    /**
     * 查询单个店铺的Alerts
     */
    public void checkShopAlerts(Shop shop) {
        if (shop.getAccessToken() == null || shop.getAccessToken().isEmpty()) {
            return;
        }

        String graphqlUrl = "https://" + shop.getShopDomain() + "/admin/api/2024-01/graphql.json";

        // GraphQL查询shop.alerts
        String query = "{ shop { alerts { action { title url } description } } }";
        String requestBody = "{\"query\": \"" + query.replace("\"", "\\\"") + "\"}";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Shopify-Access-Token", shop.getAccessToken());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    graphqlUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode alerts = root.path("data").path("shop").path("alerts");

                if (alerts.isArray() && alerts.size() > 0) {
                    for (JsonNode alert : alerts) {
                        String description = alert.path("description").asText("");
                        String actionTitle = alert.path("action").path("title").asText("");
                        String actionUrl = alert.path("action").path("url").asText("");

                        if (description.isEmpty())
                            continue;

                        // 为每个alert生成通知
                        String dedupKey = "SHOP_ALERT:" + shop.getId() + ":" +
                                description.hashCode() + ":" + LocalDate.now();

                        String content = formatShopAlertMessage(shop.getShopName(),
                                shop.getShopDomain(), description, actionTitle, actionUrl);

                        notificationService.sendAlert(AlertType.SHOP_ALERT,
                                "🔔 店铺提示 - " + shop.getShopName(),
                                content, shop.getId(), "API_POLL", dedupKey);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询Shop Alerts失败: {}", shop.getShopDomain(), e);
        }
    }

    /**
     * 格式化Shop Alert通知消息
     */
    private String formatShopAlertMessage(String shopName, String shopDomain,
            String description, String actionTitle, String actionUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 🔔 店铺提示\n\n");
        sb.append("**店铺**: ").append(shopName != null ? shopName : shopDomain).append("\n\n");
        sb.append("**提示内容**: ").append(description).append("\n\n");
        if (actionTitle != null && !actionTitle.isEmpty()) {
            sb.append("**操作建议**: ").append(actionTitle);
            if (actionUrl != null && !actionUrl.isEmpty()) {
                sb.append(" [查看详情](").append(actionUrl).append(")");
            }
            sb.append("\n\n");
        }
        sb.append("> 请登录Shopify后台查看详情。\n");
        return sb.toString();
    }
}
