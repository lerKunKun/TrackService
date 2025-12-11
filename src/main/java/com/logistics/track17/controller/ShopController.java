package com.logistics.track17.controller;

import com.logistics.track17.dto.PageResult;
import com.logistics.track17.dto.Result;
import com.logistics.track17.dto.ShopRequest;
import com.logistics.track17.dto.ShopResponse;
import com.logistics.track17.entity.Shop;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.service.ShopService;
import com.logistics.track17.service.ShopifyWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 店铺控制器
 */
@Slf4j
@RestController
@RequestMapping("/shops")
public class ShopController {

    private final ShopService shopService;
    private final ShopifyWebhookService webhookService;

    public ShopController(ShopService shopService, ShopifyWebhookService webhookService) {
        this.shopService = shopService;
        this.webhookService = webhookService;
    }

    /**
     * 获取店铺列表
     */
    @GetMapping

    public Result<PageResult<ShopResponse>> getList(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        PageResult<ShopResponse> result = shopService.getList(platform, page, pageSize);
        return Result.success(result);
    }

    /**
     * 获取店铺详情
     */
    @GetMapping("/{id}")
    public Result<ShopResponse> getById(@PathVariable Long id) {
        ShopResponse response = shopService.getById(id);
        return Result.success(response);
    }

    /**
     * 删除店铺
     */
    @DeleteMapping("/{id}")

    public Result<Void> delete(@PathVariable Long id) {
        shopService.delete(id);
        return Result.success("店铺删除成功", null);
    }

    /**
     * 验证店铺连接状态（健康检查）
     * 检查 Shopify access token 是否仍然有效
     */
    @PostMapping("/{id}/validate")
    public Result<java.util.Map<String, Object>> validateConnection(@PathVariable Long id) {
        log.info("Validating connection for shop: {}", id);
        java.util.Map<String, Object> result = shopService.validateConnection(id);
        return Result.success("连接验证完成", result);
    }

    /**
     * 批量验证所有店铺的连接状态
     */
    @PostMapping("/validate-all")
    public Result<java.util.List<java.util.Map<String, Object>>> validateAllConnections() {
        log.info("Validating all shop connections");
        java.util.List<java.util.Map<String, Object>> results = shopService.validateAllConnections();
        return Result.success("批量验证完成", results);
    }

    /**
     * 获取店铺的已注册webhooks
     */
    @GetMapping("/{id}/webhooks")
    public Result<List<Map<String, Object>>> getWebhooks(@PathVariable Long id) {
        log.info("Getting webhooks for shop: {}", id);

        ShopResponse shopResponse = shopService.getById(id);
        Shop shop = shopService.getByShopDomain(shopResponse.getShopDomain());

        if (shop == null || shop.getAccessToken() == null || shop.getShopDomain() == null) {
            throw BusinessException.of("店铺信息不完整或未授权");
        }

        List<Map<String, Object>> webhooks = webhookService.getWebhooks(shop.getShopDomain(), shop.getAccessToken());
        return Result.success("获取webhooks成功", webhooks);
    }

    /**
     * 为店铺注册所有webhooks
     */
    @PostMapping("/{id}/webhooks/register")
    public Result<Map<String, Object>> registerWebhooks(@PathVariable Long id) {
        log.info("Registering webhooks for shop: {}", id);

        ShopResponse shopResponse = shopService.getById(id);
        Shop shop = shopService.getByShopDomain(shopResponse.getShopDomain());

        if (shop == null || shop.getAccessToken() == null || shop.getShopDomain() == null) {
            throw BusinessException.of("店铺信息不完整或未授权");
        }

        Map<String, Object> result = webhookService.registerAllWebhooks(shop.getShopDomain(), shop.getAccessToken());
        return Result.success("Webhooks注册完成", result);
    }

    /**
     * 删除店铺的所有webhooks
     */
    @DeleteMapping("/{id}/webhooks")
    public Result<Map<String, Object>> deleteWebhooks(@PathVariable Long id) {
        log.info("Deleting all webhooks for shop: {}", id);

        ShopResponse shopResponse = shopService.getById(id);
        Shop shop = shopService.getByShopDomain(shopResponse.getShopDomain());

        if (shop == null || shop.getAccessToken() == null || shop.getShopDomain() == null) {
            throw BusinessException.of("店铺信息不完整或未授权");
        }

        int deletedCount = webhookService.deleteAllWebhooks(shop.getShopDomain(), shop.getAccessToken());

        Map<String, Object> result = new HashMap<>();
        result.put("shopId", id);
        result.put("shopDomain", shop.getShopDomain());
        result.put("deletedCount", deletedCount);

        return Result.success("Webhooks删除成功", result);
    }

    /**
     * 获取商店详细信息（实时从Shopify获取）
     */
    @GetMapping("/{id}/info")
    public Result<Map<String, Object>> getShopInfo(@PathVariable Long id) {
        log.info("Getting detailed shop info for: {}", id);

        ShopResponse shopResponse = shopService.getById(id);
        Shop shop = shopService.getByShopDomain(shopResponse.getShopDomain());

        if (shop == null || shop.getAccessToken() == null) {
            throw BusinessException.of("店铺未授权");
        }

        if (!"shopify".equalsIgnoreCase(shop.getPlatform())) {
            throw BusinessException.of("只支持Shopify店铺");
        }

        // 实时从Shopify获取最新信息
        Map<String, Object> shopInfo = shopService.refreshShopInfo(id).getShopInfoDetails();
        if (shopInfo == null) {
            shopInfo = new HashMap<>();
            shopInfo.put("message", "商店信息暂无详细数据");
        }

        return Result.success("获取商店信息成功", shopInfo);
    }

    /**
     * 刷新商店信息
     */
    @PostMapping("/{id}/refresh-info")
    public Result<ShopResponse> refreshShopInfo(@PathVariable Long id) {
        log.info("Refreshing shop info for: {}", id);
        ShopResponse response = shopService.refreshShopInfo(id);
        return Result.success("商店信息已刷新", response);
    }
}
