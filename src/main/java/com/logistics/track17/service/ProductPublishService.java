package com.logistics.track17.service;

import com.logistics.track17.entity.ProductShop;
import com.logistics.track17.exception.ForbiddenException;
import com.logistics.track17.mapper.ProductShopMapper;
import com.logistics.track17.util.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.List;

/**
 * Product Publish Service
 * Handles product publishing to shops with permission checks
 */
@Service
@Slf4j
public class ProductPublishService {

    @Autowired
    private ProductShopMapper productShopMapper;

    @Autowired
    private UserShopService userShopService;

    /**
     * Publish products to shops
     * 
     * @param productIds List of product IDs to publish
     * @param shopIds    List of shop IDs to publish to
     * @return Number of successful publications
     */
    @Transactional(rollbackFor = Exception.class)
    public int publishProducts(List<Long> productIds, List<Long> shopIds) {
        Long userId = UserContextHolder.getCurrentUserId();
        if (userId == null) {
            throw new ForbiddenException("User not authenticated");
        }

        int successCount = 0;

        // 1. Validate Shop Access
        for (Long shopId : shopIds) {
            if (!userShopService.hasShopAccess(userId, shopId)) {
                log.warn("User {} tried to publish to unauthorized shop {}", userId, shopId);
                throw new ForbiddenException("No permission to publish to shop: " + shopId);
            }
        }

        // 2. Publish products to shops
        for (Long productId : productIds) {

            for (Long shopId : shopIds) {
                // Check if already published/associated
                // We don't have a direct check method in mapper, but insert might fail or
                // ignore if duplicate key exists.
                // Assuming application logic should handle idempotency or delete-then-insert.
                // Let's delete existing association first to be safe or just insert ignore.
                // Standard logic: delete existing association if any, then insert new one
                // OR check existence.
                // Let's use delete-then-insert to update "published_by" and "time".

                productShopMapper.deleteByProductIdAndShopId(productId, shopId);

                ProductShop ps = new ProductShop();
                ps.setProductId(productId);
                ps.setShopId(shopId);
                ps.setPublishedBy(userId);
                ps.setPublishStatus(1); // 1 = Published
                ps.setLastExportTime(LocalDateTime.now());

                productShopMapper.insert(ps);
                successCount++;
            }
        }

        log.info("User {} published {} products to {} shops. Success count: {}",
                userId, productIds.size(), shopIds.size(), successCount);

        return successCount;
    }

    /**
     * Unpublish products from shops
     * 
     * @param productIds List of product IDs
     * @param shopIds    List of shop IDs
     */
    @Transactional(rollbackFor = Exception.class)
    public void unpublishProducts(List<Long> productIds, List<Long> shopIds) {
        Long userId = UserContextHolder.getCurrentUserId();
        if (userId == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Validate Shop Access
        for (Long shopId : shopIds) {
            if (!userShopService.hasShopAccess(userId, shopId)) {
                throw new ForbiddenException("No permission to access shop: " + shopId);
            }
        }

        for (Long productId : productIds) {

            for (Long shopId : shopIds) {
                productShopMapper.deleteByProductIdAndShopId(productId, shopId);
            }
        }

        log.info("User {} unpublished {} products from {} shops", userId, productIds.size(), shopIds.size());
    }
}
