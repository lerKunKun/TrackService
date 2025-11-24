package com.logistics.track17.controller;

import com.logistics.track17.entity.Shop;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.service.ShopService;
import com.logistics.track17.service.ShopifyOAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Shopify OAuth控制器
 */
@Slf4j
@RestController
@RequestMapping("/oauth/shopify")
public class ShopifyOAuthController {

    private final ShopifyOAuthService shopifyOAuthService;
    private final ShopService shopService;

    @Value("${shopify.oauth.frontend-redirect:http://localhost:5173/shops}")
    private String frontendRedirect;

    // 临时存储state，生产环境应使用Redis
    private final Map<String, String> stateStore = new HashMap<>();

    public ShopifyOAuthController(ShopifyOAuthService shopifyOAuthService,
                                   ShopService shopService) {
        this.shopifyOAuthService = shopifyOAuthService;
        this.shopService = shopService;
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

        // 提取state并保存（用于回调验证）
        String state = extractStateFromUrl(authUrl);
        stateStore.put(state, shopDomain);

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
            // 1. 验证state
            String storedShop = stateStore.get(state);
            if (storedShop == null || !storedShop.equals(shop)) {
                log.warn("Invalid OAuth state: {}", state);
                response.sendRedirect(frontendRedirect + "?oauth=error&reason=invalid_state");
                return;
            }
            stateStore.remove(state);  // 使用后移除

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

            // 4. 保存或更新店铺信息
            Shop existingShop = shopService.getByShopDomain(shop);

            if (existingShop != null) {
                // 更新现有店铺
                existingShop.setAccessToken(accessToken);
                existingShop.setOauthScope(scope);
                existingShop.setOauthState(state);
                existingShop.setShopDomain(shop);
                existingShop.setTokenExpiresAt(null);  // Shopify token不过期
                shopService.update(existingShop);

                log.info("Updated existing shop: {}", shop);
            } else {
                // 创建新店铺
                Shop newShop = new Shop();
                newShop.setShopName(shop.replace(".myshopify.com", ""));
                newShop.setPlatform("shopify");
                newShop.setStoreUrl("https://" + shop);
                newShop.setShopDomain(shop);
                newShop.setAccessToken(accessToken);
                newShop.setOauthScope(scope);
                newShop.setOauthState(state);
                newShop.setTokenExpiresAt(null);  // Shopify token不过期
                shopService.create(newShop);

                log.info("Created new shop: {}", shop);
            }

            // 5. 重定向回前端，显示成功消息
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
