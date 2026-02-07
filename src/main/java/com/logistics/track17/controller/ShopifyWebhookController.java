package com.logistics.track17.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.track17.entity.Order;
import com.logistics.track17.entity.Shop;
import com.logistics.track17.dto.TrackingRequest;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.service.OrderService;
import com.logistics.track17.service.ShopService;
import com.logistics.track17.service.ShopifyWebhookService;
import com.logistics.track17.service.TrackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Shopify Webhook 控制器
 * 接收和处理来自Shopify的webhook回调
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/shopify")
public class ShopifyWebhookController {

    private final ShopifyWebhookService webhookService;
    private final ShopService shopService;
    private final OrderService orderService;
    private final TrackingService trackingService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ShopifyWebhookController(ShopifyWebhookService webhookService,
            ShopService shopService,
            OrderService orderService,
            TrackingService trackingService,
            ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.shopService = shopService;
        this.orderService = orderService;
        this.trackingService = trackingService;
        this.objectMapper = objectMapper;
    }

    /**
     * 处理店铺信息更新webhook
     * Topic: shop/update
     */
    @PostMapping("/shop-update")
    public ResponseEntity<Void> handleShopUpdate(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Shop-Domain") String shopDomain,
            @RequestHeader("X-Shopify-Hmac-SHA256") String hmac,
            @RequestHeader(value = "X-Shopify-Topic", required = false) String topic) {

        log.info("Received webhook: shop/update from: {}", shopDomain);

        // 验证webhook签名
        if (!webhookService.verifyWebhookSignature(payload, hmac)) {
            log.warn("Invalid webhook signature for shop/update from: {}", shopDomain);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 解析店铺信息
            JsonNode shopData = objectMapper.readTree(payload);

            // 查找店铺
            Shop shop = shopService.getByShopDomain(shopDomain);
            if (shop == null) {
                log.warn("Shop not found for domain: {}", shopDomain);
                return ResponseEntity.ok().build(); // 返回200避免Shopify重试
            }

            // 更新店铺信息
            if (shopData.has("name")) {
                shop.setShopName(shopData.get("name").asText());
            }
            if (shopData.has("email")) {
                // 可以添加email字段到Shop实体
                log.info("Shop email: {}", shopData.get("email").asText());
            }
            if (shopData.has("iana_timezone")) {
                shop.setTimezone(shopData.get("iana_timezone").asText());
            }
            if (shopData.has("domain")) {
                shop.setStoreUrl("https://" + shopData.get("domain").asText());
            }

            shop.setLastSyncTime(LocalDateTime.now());
            shopService.update(shop);

            log.info("Successfully updated shop info for: {}", shopDomain);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing shop/update webhook for: {}", shopDomain, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 处理App卸载webhook
     * Topic: app/uninstalled
     */
    @PostMapping("/app-uninstalled")
    public ResponseEntity<Void> handleAppUninstalled(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Shop-Domain") String shopDomain,
            @RequestHeader("X-Shopify-Hmac-SHA256") String hmac,
            @RequestHeader(value = "X-Shopify-Topic", required = false) String topic) {

        log.info("Received webhook: app/uninstalled from: {}", shopDomain);

        // 验证webhook签名
        if (!webhookService.verifyWebhookSignature(payload, hmac)) {
            log.warn("Invalid webhook signature for app/uninstalled from: {}", shopDomain);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 查找店铺
            Shop shop = shopService.getByShopDomain(shopDomain);
            if (shop == null) {
                log.warn("Shop not found for domain: {}", shopDomain);
                return ResponseEntity.ok().build();
            }

            // 标记店铺为失效
            shop.setConnectionStatus("invalid");
            shop.setIsActive(false);
            shop.setLastSyncTime(LocalDateTime.now());
            shopService.update(shop);

            log.info("Marked shop as uninstalled: {}", shopDomain);

            // TODO: 发送通知给管理员或店铺所有者

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing app/uninstalled webhook for: {}", shopDomain, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 处理订单创建webhook
     * Topic: orders/create
     */
    @PostMapping("/orders-create")
    public ResponseEntity<Void> handleOrdersCreate(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Shop-Domain") String shopDomain,
            @RequestHeader("X-Shopify-Hmac-SHA256") String hmac,
            @RequestHeader(value = "X-Shopify-Topic", required = false) String topic) {

        log.info("Received webhook: orders/create from: {}", shopDomain);

        // 验证webhook签名
        if (!webhookService.verifyWebhookSignature(payload, hmac)) {
            log.warn("Invalid webhook signature for orders/create from: {}", shopDomain);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 解析订单数据
            JsonNode orderData = objectMapper.readTree(payload);

            // 查找店铺
            Shop shop = shopService.getByShopDomain(shopDomain);
            if (shop == null) {
                log.warn("Shop not found for domain: {}", shopDomain);
                return ResponseEntity.ok().build();
            }

            // 保存订单（委托给OrderService）
            Order order = orderService.saveOrderFromWebhook(shop, orderData);

            log.info("Order synced successfully: {} (ID: {}) for shop: {}",
                    order.getOrderNumber(), order.getId(), shopDomain);

            // 更新店铺最后同步时间
            shop.setLastSyncTime(LocalDateTime.now());
            shopService.update(shop);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing orders/create webhook for: {}", shopDomain, e);
            // 返回200避免Shopify重试，错误已记录
            return ResponseEntity.ok().build();
        }
    }

    /**
     * 处理订单更新webhook
     * Topic: orders/updated
     */
    @PostMapping("/orders-updated")
    public ResponseEntity<Void> handleOrdersUpdated(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Shop-Domain") String shopDomain,
            @RequestHeader("X-Shopify-Hmac-SHA256") String hmac,
            @RequestHeader(value = "X-Shopify-Topic", required = false) String topic) {

        log.info("Received webhook: orders/updated from: {}", shopDomain);

        // 验证webhook签名
        if (!webhookService.verifyWebhookSignature(payload, hmac)) {
            log.warn("Invalid webhook signature for orders/updated from: {}", shopDomain);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 解析订单数据
            JsonNode orderData = objectMapper.readTree(payload);

            // 查找店铺
            Shop shop = shopService.getByShopDomain(shopDomain);
            if (shop == null) {
                log.warn("Shop not found for domain: {}", shopDomain);
                return ResponseEntity.ok().build();
            }

            // 提取订单信息
            String orderNumber = orderData.has("order_number") ? orderData.get("order_number").asText() : "unknown";
            Long orderId = orderData.has("id") ? orderData.get("id").asLong() : null;
            String fulfillmentStatus = orderData.has("fulfillment_status")
                    ? orderData.get("fulfillment_status").asText()
                    : null;

            log.info("Order updated: {} (ID: {}, Status: {}) for shop: {}",
                    orderNumber, orderId, fulfillmentStatus, shopDomain);

            Order order = orderService.saveOrderFromWebhook(shop, orderData);

            // 检查是否有物流信息
            if (orderData.has("fulfillments") && orderData.get("fulfillments").isArray()) {
                JsonNode fulfillments = orderData.get("fulfillments");
                for (JsonNode fulfillment : fulfillments) {
                    if (fulfillment.has("tracking_number")) {
                        String trackingNumber = fulfillment.get("tracking_number").asText();
                        String trackingCompany = fulfillment.has("tracking_company")
                                ? fulfillment.get("tracking_company").asText()
                                : null;

                        log.info("Tracking info found - Number: {}, Company: {}",
                                trackingNumber, trackingCompany);

                        TrackingRequest trackingRequest = new TrackingRequest();
                        trackingRequest.setTrackingNumber(trackingNumber);
                        trackingRequest.setOrderId(order.getId());
                        trackingRequest.setSource("shopify");
                        try {
                            trackingService.create(trackingRequest);
                        } catch (BusinessException e) {
                            log.warn("Unable to register tracking number {} from Shopify webhook: {}",
                                    trackingNumber, e.getMessage());
                        }
                    }
                }
            }

            // 更新店铺最后同步时间
            shop.setLastSyncTime(LocalDateTime.now());
            shopService.update(shop);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing orders/updated webhook for: {}", shopDomain, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Webhook健康检查端点
     * 用于测试webhook配置是否正确
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Shopify webhooks endpoint is healthy");
    }
}
