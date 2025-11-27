package com.logistics.track17.service;

import com.logistics.track17.dto.PageResult;
import com.logistics.track17.dto.ShopRequest;
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
     * 创建店铺
     */
    @Transactional(rollbackFor = Exception.class)
    public ShopResponse create(ShopRequest request) {
        log.info("Creating shop: {}", request.getShopName());

        // 验证平台类型
        if (!isValidPlatform(request.getPlatform())) {
            throw BusinessException.of("不支持的平台类型: " + request.getPlatform());
        }

        // TODO: 验证API配置有效性（调用平台API测试）

        Shop shop = new Shop();
        BeanUtils.copyProperties(request, shop);

        // 设置默认时区
        if (StringUtils.isBlank(shop.getTimezone())) {
            shop.setTimezone("UTC");
        }

        shopMapper.insert(shop);

        log.info("Shop created successfully with ID: {}", shop.getId());
        return convertToResponse(shop);
    }

    /**
     * 创建店铺（直接使用Shop对象，用于OAuth流程）
     */
    @Transactional(rollbackFor = Exception.class)
    public Shop create(Shop shop) {
        log.info("Creating shop from OAuth: {}", shop.getShopName());

        // 设置默认时区
        if (StringUtils.isBlank(shop.getTimezone())) {
            shop.setTimezone("UTC");
        }

        shopMapper.insert(shop);

        log.info("Shop created successfully with ID: {}", shop.getId());
        return shop;
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
     * 更新店铺
     */
    @Transactional(rollbackFor = Exception.class)
    public ShopResponse update(Long id, ShopRequest request) {
        log.info("Updating shop: {}", id);

        Shop existingShop = shopMapper.selectById(id);
        if (existingShop == null) {
            throw BusinessException.of(404, "店铺不存在");
        }

        // 验证平台类型
        if (request.getPlatform() != null && !isValidPlatform(request.getPlatform())) {
            throw BusinessException.of("不支持的平台类型: " + request.getPlatform());
        }

        Shop shop = new Shop();
        BeanUtils.copyProperties(request, shop);
        shop.setId(id);

        shopMapper.update(shop);

        log.info("Shop updated successfully: {}", id);
        return getById(id);
    }

    /**
     * 删除店铺
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("Deleting shop: {}", id);

        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw BusinessException.of(404, "店铺不存在");
        }

        // 检查是否有关联订单
        Long orderCount = shopMapper.countOrdersByShopId(id);
        if (orderCount > 0) {
            throw BusinessException.of("该店铺下有 " + orderCount + " 个订单，无法删除");
        }

        shopMapper.deleteById(id);
        log.info("Shop deleted successfully: {}", id);
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
}
