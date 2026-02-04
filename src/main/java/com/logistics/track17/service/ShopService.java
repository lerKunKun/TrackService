package com.logistics.track17.service;

import com.logistics.track17.dto.PageResult;
import com.logistics.track17.dto.ShopResponse;
import com.logistics.track17.entity.Shop;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.mapper.ShopMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 店铺服务
 */
@Slf4j
@Service
public class ShopService {

    private final ShopMapper shopMapper;
    private final ShopifyOAuthService shopifyOAuthService;

    public ShopService(ShopMapper shopMapper, ShopifyOAuthService shopifyOAuthService) {
        this.shopMapper = shopMapper;
        this.shopifyOAuthService = shopifyOAuthService;
    }

    /**
     * 创建店铺（直接使用Shop对象，用于OAuth流程）
     */
    /**
     * 创建店铺（直接使用Shop对象，用于OAuth流程）
     */
    @Transactional(rollbackFor = Exception.class)
    public Shop create(Shop shop) {
        log.info("Creating shop: {}", shop.getShopName());

        // 设置默认时区
        if (StringUtils.isBlank(shop.getTimezone())) {
            shop.setTimezone("UTC");
        }

        try {
            shopMapper.insert(shop);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("Shop create failed due to duplicate key, trying update instead: {}", shop.getShopDomain());
            Shop existing = getByShopDomain(shop.getShopDomain());
            if (existing != null) {
                shop.setId(existing.getId());
                update(shop);
                return shop;
            } else {
                throw e;
            }
        }

        log.info("Shop created successfully with ID: {}", shop.getId());
        return shop;
    }

    /**
     * 保存或更新店铺 (Upsert logic)
     */
    @Transactional(rollbackFor = Exception.class)
    public Shop saveOrUpdateShop(Shop shop) {
        if (StringUtils.isBlank(shop.getShopDomain())) {
            throw BusinessException.of("Shop domain is required");
        }

        Shop existingShop = getByShopDomain(shop.getShopDomain());

        if (existingShop != null) {
            log.info("Shop exists, updating: {}", shop.getShopDomain());
            shop.setId(existingShop.getId());
            // Preserve fields that shouldn't be overwritten blindly if new is null
            if (shop.getUserId() == null)
                shop.setUserId(existingShop.getUserId());

            update(shop);
            return shop;
        } else {
            return create(shop);
        }
    }

    /**
     * 根据店铺域名查询
     */
    public Shop getByShopDomain(String shopDomain) {
        return shopMapper.selectByShopDomain(shopDomain);
    }

    /**
     * 更新店铺（直接使用Shop对象，用于OAuth流程）
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Shop shop) {
        log.info("Updating shop: {}", shop.getId());
        shopMapper.update(shop);
    }

    /**
     * 获取店铺列表（分页）
     */
    public PageResult<ShopResponse> getList(String platform, Integer page, Integer pageSize) {
        page = page == null || page < 1 ? 1 : page;
        pageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);

        int offset = (page - 1) * pageSize;

        List<Shop> shops = shopMapper.selectList(platform, offset, pageSize);
        Long total = shopMapper.count(platform);

        List<ShopResponse> responses = shops.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 填充订单数量
        responses.forEach(response -> {
            Long orderCount = shopMapper.countOrdersByShopId(response.getId());
            response.setOrderCount(orderCount);
        });

        return PageResult.of(responses, total, page, pageSize);
    }

    /**
     * 获取店铺详情
     */
    public ShopResponse getById(Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw BusinessException.of(404, "店铺不存在");
        }
        return convertToResponse(shop);
    }

    /**
     * 删除店铺（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("Soft deleting shop: {}", id);

        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw BusinessException.of(404, "店铺不存在");
        }

        // 检查是否有关联订单
        Long orderCount = shopMapper.countOrdersByShopId(id);
        if (orderCount > 0) {
            throw BusinessException.of("该店铺下有 " + orderCount + " 个订单，无法删除");
        }

        // 软删除：设置 deleted_at 时间戳
        shopMapper.deleteById(id);
        log.info("Shop soft deleted successfully: {}", id);
    }

    /**
     * 验证平台类型
     */
    private boolean isValidPlatform(String platform) {
        return "shopify".equalsIgnoreCase(platform)
                || "shopline".equalsIgnoreCase(platform)
                || "tiktokshop".equalsIgnoreCase(platform);
    }

    /**
     * 转换为响应DTO
     */
    private ShopResponse convertToResponse(Shop shop) {
        ShopResponse response = new ShopResponse();
        BeanUtils.copyProperties(shop, response);

        // 隐藏敏感信息，只显示前4位
        if (StringUtils.isNotBlank(shop.getApiKey())) {
            response.setApiKey(maskSecret(shop.getApiKey()));
        }

        // 如果有shopInfoJson，解析为Map
        if (StringUtils.isNotBlank(shop.getShopInfoJson())) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> details = mapper.readValue(
                        shop.getShopInfoJson(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                        });
                response.setShopInfoDetails(details);
            } catch (Exception e) {
                log.warn("Failed to parse shop info JSON for shop: {}", shop.getId(), e);
            }
        }

        return response;
    }

    /**
     * 隐藏密钥信息
     */
    private String maskSecret(String secret) {
        if (StringUtils.isBlank(secret) || secret.length() <= 4) {
            return "****";
        }
        return secret.substring(0, 4) + "****";
    }

    /**
     * 验证店铺连接状态（健康检查）
     * 检查 access token 是否仍然有效
     *
     * @param id 店铺ID
     * @return 连接状态信息
     */
    @Transactional(rollbackFor = Exception.class)
    public java.util.Map<String, Object> validateConnection(Long id) {
        log.info("Validating connection for shop: {}", id);

        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw BusinessException.of(404, "店铺不存在");
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("shopId", id);
        result.put("shopName", shop.getShopName());
        result.put("platform", shop.getPlatform());

        // 只有 Shopify 平台才需要验证 token
        if (!"shopify".equalsIgnoreCase(shop.getPlatform())) {
            result.put("status", "unsupported");
            result.put("message", "该平台不支持连接验证");
            return result;
        }

        if (StringUtils.isBlank(shop.getAccessToken()) || StringUtils.isBlank(shop.getShopDomain())) {
            result.put("status", "invalid");
            result.put("message", "缺少访问令牌或店铺域名");
            shop.setConnectionStatus("invalid");
            shopMapper.update(shop);
            return result;
        }

        // 验证 token 是否有效
        boolean isValid = shopifyOAuthService.validateAccessToken(shop.getShopDomain(), shop.getAccessToken());

        if (isValid) {
            result.put("status", "active");
            result.put("message", "连接正常");
            shop.setConnectionStatus("active");
            shop.setLastValidatedAt(java.time.LocalDateTime.now());
        } else {
            result.put("status", "invalid");
            result.put("message", "访问令牌已失效，请重新授权");
            shop.setConnectionStatus("invalid");
        }

        shopMapper.update(shop);

        result.put("tokenType", shop.getTokenType());
        result.put("lastValidatedAt", shop.getLastValidatedAt());

        log.info("Connection validation completed for shop {}: {}", id, result.get("status"));
        return result;
    }

    /**
     * 批量验证所有店铺的连接状态
     *
     * @return 验证结果列表
     */
    public List<java.util.Map<String, Object>> validateAllConnections() {
        log.info("Validating all shop connections");

        List<Shop> shops = shopMapper.selectList(null, null, null);
        List<java.util.Map<String, Object>> results = new java.util.ArrayList<>();

        for (Shop shop : shops) {
            try {
                java.util.Map<String, Object> result = validateConnection(shop.getId());
                results.add(result);
            } catch (Exception e) {
                log.error("Failed to validate connection for shop: {}", shop.getId(), e);
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("shopId", shop.getId());
                result.put("status", "error");
                result.put("message", e.getMessage());
                results.add(result);
            }
        }

        log.info("Completed validation for {} shops", results.size());
        return results;
    }

    /**
     * 刷新Shopify商店信息
     *
     * @param shopId 商店ID
     * @return 更新后的商店信息
     */
    @Transactional(rollbackFor = Exception.class)
    public ShopResponse refreshShopInfo(Long shopId) {
        log.info("Refreshing shop info for: {}", shopId);

        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw BusinessException.of(404, "店铺不存在");
        }

        if (!"shopify".equalsIgnoreCase(shop.getPlatform())) {
            throw BusinessException.of("只支持Shopify店铺刷新");
        }

        if (StringUtils.isBlank(shop.getAccessToken()) || StringUtils.isBlank(shop.getShopDomain())) {
            throw BusinessException.of("店铺未授权或缺少店铺域名");
        }

        // 调用Shopify API获取最新信息
        java.util.Map<String, Object> shopInfo;
        try {
            shopInfo = shopifyOAuthService.getShopInfo(shop.getShopDomain(), shop.getAccessToken());
        } catch (Exception e) {
            log.error("Failed to fetch shop info from Shopify", e);
            shop.setConnectionStatus("invalid");
            shopMapper.update(shop);
            throw BusinessException.of("获取商店信息失败: " + e.getMessage());
        }

        // 更新Shop实体
        updateShopFromInfo(shop, shopInfo);
        shop.setConnectionStatus("active");
        shop.setLastValidatedAt(java.time.LocalDateTime.now());
        shopMapper.update(shop);

        log.info("Successfully refreshed shop info for: {}", shopId);
        return convertToResponse(shop);
    }

    /**
     * 从Shopify返回的信息更新Shop实体
     */
    private void updateShopFromInfo(Shop shop, java.util.Map<String, Object> shopInfo) {
        if (shopInfo.get("name") != null) {
            shop.setShopName((String) shopInfo.get("name"));
        }
        if (shopInfo.get("contactEmail") != null) {
            shop.setContactEmail((String) shopInfo.get("contactEmail"));
        }
        if (shopInfo.get("email") != null) {
            shop.setOwnerEmail((String) shopInfo.get("email"));
        }
        if (shopInfo.get("currencyCode") != null) {
            shop.setCurrency((String) shopInfo.get("currencyCode"));
        }
        if (shopInfo.get("planDisplayName") != null) {
            shop.setPlanDisplayName((String) shopInfo.get("planDisplayName"));
        }
        if (shopInfo.get("shopifyPlus") != null) {
            shop.setIsShopifyPlus((Boolean) shopInfo.get("shopifyPlus"));
        }
        if (shopInfo.get("ianaTimezone") != null) {
            shop.setIanaTimezone((String) shopInfo.get("ianaTimezone"));
            shop.setTimezone((String) shopInfo.get("ianaTimezone")); // 同时更新timezone字段
        }
        if (shopInfo.get("primaryDomainUrl") != null) {
            shop.setPrimaryDomain((String) shopInfo.get("primaryDomainUrl"));
        }

        // 保存完整的JSON数据
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            shop.setShopInfoJson(mapper.writeValueAsString(shopInfo));
        } catch (Exception e) {
            log.warn("Failed to serialize shop info to JSON", e);
        }
    }
}
