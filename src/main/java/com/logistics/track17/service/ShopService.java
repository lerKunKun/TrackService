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

    public ShopService(ShopMapper shopMapper) {
        this.shopMapper = shopMapper;
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
}
