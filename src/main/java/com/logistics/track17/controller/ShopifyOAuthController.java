package com.logistics.track17.controller;

import com.logistics.track17.entity.Shop;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.service.ShopService;
import com.logistics.track17.service.ShopifyOAuthService;
import com.logistics.track17.service.ShopifyWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Shopify OAuth控制器
 */
@Slf4j
@RestController
@RequestMapping("/oauth/shopify")
public class ShopifyOAuthController {

    private final ShopifyOAuthService shopifyOAuthService;
    private final ShopService shopService;
    private final ShopifyWebhookService webhookService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${shopify.oauth.frontend-redirect:http://localhost:5173/shops}")
    private String frontendRedirect;

    // OAuth state 存储配置
    private static final String OAUTH_STATE_PREFIX = "oauth:state:";
    private static final long STATE_EXPIRE_SECONDS = 300; // 5分钟过期

    public ShopifyOAuthController(ShopifyOAuthService shopifyOAuthService,
                                   ShopService shopService,
                                   ShopifyWebhookService webhookService) {
        this.shopifyOAuthService = shopifyOAuthService;
        this.shopService = shopService;
        this.webhookService = webhookService;
    }

    /**
     * 开始OAuth授权流程
     *
     * @param shopDomain Shopify店铺域名
     * @return 重定向到Shopify授权页面
     */
    @GetMapping("/authorize")
    public void authorize(@RequestParam String shopDomain,
                          HttpServletResponse response) throws IOException {
        log.info("Starting Shopify OAuth for shop: {}", shopDomain);

        // 验证域名格式
        if (!shopifyOAuthService.isValidShopDomain(shopDomain)) {
            throw BusinessException.of("无效的Shopify店铺域名格式");
        }

        // 生成授权URL
        String authUrl = shopifyOAuthService.generateAuthorizationUrl(shopDomain);

        // 提取state并保存到Redis（5分钟过期）
        String state = extractStateFromUrl(authUrl);
        String key = OAUTH_STATE_PREFIX + state;
        redisTemplate.opsForValue().set(key, shopDomain, STATE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.info("Stored OAuth state in Redis: {} for shop: {}", state, shopDomain);

        // 重定向到Shopify授权页面
        response.sendRedirect(authUrl);
    }

    /**
     * OAuth回调处理
     *
     * @param code  授权码
     * @param shop  店铺域名
     * @param state OAuth state
     * @param hmac  HMAC签名
     * @return 授权结果
     */
    @GetMapping("/callback")
    public void callback(
            @RequestParam String code,
            @RequestParam String shop,
            @RequestParam String state,
            @RequestParam String hmac,
            @RequestParam(required = false) String timestamp,
            HttpServletResponse response) throws IOException {

        log.info("Received OAuth callback from shop: {}", shop);

        try {
            // 1. 从Redis验证state
            String key = OAUTH_STATE_PREFIX + state;
            String storedShop = (String) redisTemplate.opsForValue().get(key);

            if (storedShop == null) {
                log.warn("Invalid or expired OAuth state: {}", state);
                response.sendRedirect(frontendRedirect + "?oauth=error&reason=invalid_or_expired_state");
                return;
            }

            if (!storedShop.equals(shop)) {
                log.warn("OAuth state mismatch. Stored: {}, Received: {}", storedShop, shop);
                response.sendRedirect(frontendRedirect + "?oauth=error&reason=state_mismatch");
                return;
            }

            // 删除已使用的state
            redisTemplate.delete(key);
            log.info("OAuth state validated and removed from Redis: {}", state);

            // 2. 验证HMAC签名
            Map<String, String> params = new HashMap<>();
            params.put("code", code);
            params.put("shop", shop);
            params.put("state", state);
            params.put("hmac", hmac);
            if (timestamp != null) {
                params.put("timestamp", timestamp);
            }

            if (!shopifyOAuthService.validateHmac(params)) {
                log.warn("HMAC validation failed for shop: {}", shop);
                response.sendRedirect(frontendRedirect + "?oauth=error&reason=hmac_failed");
                return;
            }

            // 3. 使用授权码换取访问令牌
            Map<String, Object> tokenInfo = shopifyOAuthService.exchangeCodeForToken(code, shop);
            String accessToken = (String) tokenInfo.get("access_token");
            String scope = (String) tokenInfo.get("scope");

            log.info("Successfully obtained access token for shop: {}", shop);

            // 4. 获取店铺详细信息
            Map<String, Object> shopInfo;
            try {
                shopInfo = shopifyOAuthService.getShopInfo(shop, accessToken);
                log.info("Successfully fetched shop info: {}", shopInfo.get("name"));
            } catch (Exception e) {
                log.error("Failed to fetch shop info, using basic info", e);
                shopInfo = new HashMap<>();
                shopInfo.put("name", shop.replace(".myshopify.com", ""));
                shopInfo.put("myshopify_domain", shop);
                shopInfo.put("timezone", "UTC");
            }

            // 5. 保存或更新店铺信息
            Shop existingShop = shopService.getByShopDomain(shop);

            if (existingShop != null) {
                // 更新现有店铺
                existingShop.setShopName((String) shopInfo.getOrDefault("name", existingShop.getShopName()));
                existingShop.setAccessToken(accessToken);
                existingShop.setTokenType("offline");  // 使用 offline token
                existingShop.setConnectionStatus("active");  // 连接状态正常
                existingShop.setLastValidatedAt(LocalDateTime.now());
                existingShop.setOauthScope(scope);
                existingShop.setOauthState(state);
                existingShop.setShopDomain(shop);
                existingShop.setStoreUrl("https://" + shop);
                existingShop.setTimezone((String) shopInfo.getOrDefault("timezone", "UTC"));
                existingShop.setTokenExpiresAt(null);  // Offline token 永不过期
                existingShop.setIsActive(true);
                shopService.update(existingShop);

                log.info("Updated existing shop: {} with offline token", shop);
            } else {
                // 创建新店铺 - 需要设置 userId（临时使用 ID=1 的管理员）
                Shop newShop = new Shop();
                newShop.setUserId(1L);  // TODO: 从当前登录用户获取
                newShop.setShopName((String) shopInfo.getOrDefault("name", shop.replace(".myshopify.com", "")));
                newShop.setPlatform("shopify");
                newShop.setStoreUrl("https://" + shop);
                newShop.setShopDomain(shop);
                newShop.setTimezone((String) shopInfo.getOrDefault("timezone", "UTC"));
                newShop.setAccessToken(accessToken);
                newShop.setTokenType("offline");  // 使用 offline token
                newShop.setConnectionStatus("active");  // 连接状态正常
                newShop.setLastValidatedAt(LocalDateTime.now());
                newShop.setOauthScope(scope);
                newShop.setOauthState(state);
                newShop.setTokenExpiresAt(null);  // Offline token 永不过期
                newShop.setIsActive(true);
                shopService.create(newShop);

                log.info("Created new shop: {} with offline token", shop);
            }

            // 6. 注册Webhooks (异步执行,不阻塞OAuth流程)
            try {
                Map<String, Object> webhookResult = webhookService.registerAllWebhooks(shop, accessToken);
                log.info("Webhook registration result for {}: {} success, {} failed",
                        shop, webhookResult.get("totalSuccess"), webhookResult.get("totalFailed"));
            } catch (Exception webhookError) {
                log.error("Failed to register webhooks for shop: {}, but OAuth succeeded", shop, webhookError);
                // Webhook注册失败不影响OAuth流程
            }

            // 7. 重定向回前端，显示成功消息
            response.sendRedirect(frontendRedirect + "?oauth=success");

        } catch (Exception e) {
            log.error("OAuth callback failed for shop: {}", shop, e);
            response.sendRedirect(frontendRedirect + "?oauth=error&reason=" + e.getMessage());
        }
    }

    /**
     * 从授权URL中提取state参数
     */
    private String extractStateFromUrl(String url) {
        String[] parts = url.split("state=");
        if (parts.length > 1) {
            return parts[1].split("&")[0];
        }
        throw BusinessException.of("无法提取OAuth state");
    }
}
