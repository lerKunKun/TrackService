package com.logistics.track17.controller;

import com.logistics.track17.entity.Shop;
import com.logistics.track17.entity.User;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.service.ShopService;
import com.logistics.track17.service.ShopifyOAuthService;
import com.logistics.track17.service.ShopifyWebhookService;
import com.logistics.track17.service.UserService;
import com.logistics.track17.util.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final UserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${shopify.oauth.frontend-redirect:http://localhost:5173/shops}")
    private String frontendRedirect;

    // OAuth state 存储配置
    private static final String OAUTH_STATE_PREFIX = "oauth:state:";
    private static final long STATE_EXPIRE_SECONDS = 300; // 5分钟过期

    public ShopifyOAuthController(ShopifyOAuthService shopifyOAuthService,
            ShopService shopService,
            ShopifyWebhookService webhookService,
            UserService userService) {
        this.shopifyOAuthService = shopifyOAuthService;
        this.shopService = shopService;
        this.webhookService = webhookService;
        this.userService = userService;
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
            @RequestParam(required = false) String host,
            HttpServletRequest request,
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

            // 2. 验证HMAC签名（使用原始query string）
            String queryString = request.getQueryString();
            if (!shopifyOAuthService.validateHmacFromQueryString(queryString)) {
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
            // 5. 保存或更新店铺信息
            Shop shopEntity = new Shop();
            // 如果已存在将被覆盖，如果不存在将被创建
            shopEntity.setShopDomain(shop);

            // 设置公共字段
            shopEntity.setShopName((String) shopInfo.getOrDefault("name", shop.replace(".myshopify.com", "")));
            shopEntity.setPlatform("shopify");
            shopEntity.setStoreUrl("https://" + shop);

            // 认证信息
            shopEntity.setAccessToken(accessToken);
            shopEntity.setTokenType("offline");
            shopEntity.setOauthScope(scope);
            shopEntity.setOauthState(state);
            shopEntity.setConnectionStatus("active");
            shopEntity.setLastValidatedAt(LocalDateTime.now());
            shopEntity.setIsActive(true);
            shopEntity.setTokenExpiresAt(null);

            // 详细信息
            shopEntity.setTimezone((String) shopInfo.getOrDefault("timezone", "UTC"));
            shopEntity.setCurrency((String) shopInfo.get("currency"));
            shopEntity.setPlanName((String) shopInfo.get("plan_name"));
            shopEntity.setPlanDisplayName((String) shopInfo.get("plan_display_name"));
            shopEntity.setIsShopifyPlus((Boolean) shopInfo.getOrDefault("shopify_plus", false));
            shopEntity.setPrimaryDomain((String) shopInfo.get("primary_domain"));
            shopEntity.setShopOwner((String) shopInfo.get("shop_owner"));
            shopEntity.setContactEmail((String) shopInfo.get("email"));
            shopEntity.setOwnerEmail((String) shopInfo.get("email"));
            if (shopInfo.get("iana_timezone") != null) {
                shopEntity.setIanaTimezone((String) shopInfo.get("iana_timezone"));
            }

            // 如果是新店铺，需要设置默认UserId
            Shop existing = shopService.getByShopDomain(shop);
            if (existing == null) {
                Long currentUserId = UserContextHolder.getCurrentUserId();
                if (currentUserId != null) {
                    shopEntity.setUserId(currentUserId);
                } else {
                    User adminUser = userService.getUserByUsername("admin");
                    if (adminUser != null) {
                        shopEntity.setUserId(adminUser.getId());
                        log.info("Assigned Shopify shop to admin user {} due to missing user context.", adminUser.getId());
                    } else {
                        throw BusinessException.of("No authenticated user or admin user found for Shopify OAuth callback");
                    }
                }
            }

            // 调用Service层的saveOrUpdateShop方法，统一处理重复逻辑
            shopService.saveOrUpdateShop(shopEntity);

            log.info("Shop saved successfully: {}", shop);

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
            String reason = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(frontendRedirect + "?oauth=error&reason=" + reason);
        }
    }

    /**
     * 从授权URL中提取state参数
     */
    private String extractStateFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();
            if (query == null) {
                throw BusinessException.of("无法提取OAuth state");
            }
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && "state".equals(kv[0])) {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        } catch (URISyntaxException e) {
            throw BusinessException.of("无法解析OAuth URL");
        }
        throw BusinessException.of("无法提取OAuth state");
    }
}
