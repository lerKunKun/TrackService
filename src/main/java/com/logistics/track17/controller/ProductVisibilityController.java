package com.logistics.track17.controller;

import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.Product;
import com.logistics.track17.service.ProductVisibilityService;
import com.logistics.track17.util.ShopContext;
import com.logistics.track17.util.UserContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Product Visibility Controller
 * Managing product authorization and visibility queries
 */
@RestController
@RequestMapping("/product-visibility")
public class ProductVisibilityController {

    @Autowired
    private ProductVisibilityService productVisibilityService;

    /**
     * Get authorized products for current user
     * Supports optional shop context filtering
     */
    @GetMapping("/products")
    public Result<Object> getAuthorizedProducts(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) List<Long> filterShopIds,
            @RequestParam(required = false) String procurementStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = UserContextHolder.getCurrentUserId();

        // If shopId not provided via param but exists in context (header), use it
        if (shopId == null && ShopContext.hasShopContext()) {
            shopId = ShopContext.getCurrentShopId();
        }

        List<Product> products = productVisibilityService.getAuthorizedProducts(userId, shopId, keyword, tags,
                filterShopIds, procurementStatus, page, size);
        return Result.success(products);
    }

    /**
     * Count authorized products
     */
    @GetMapping("/products/count")
    public Result<Object> countAuthorizedProducts(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) List<Long> filterShopIds,
            @RequestParam(required = false) String procurementStatus) {

        Long userId = UserContextHolder.getCurrentUserId();
        if (shopId == null && ShopContext.hasShopContext()) {
            shopId = ShopContext.getCurrentShopId();
        }

        int count = productVisibilityService.countAuthorizedProducts(userId, shopId, keyword, tags, filterShopIds,
                procurementStatus);
        return Result.success(count);
    }

    /**
     * Check visibility for specific product
     */
    @GetMapping("/products/{productId}/check")
    public Result<Object> checkProductVisibility(
            @PathVariable Long productId,
            @RequestParam(required = false) Long shopId) {

        Long userId = UserContextHolder.getCurrentUserId();
        if (shopId == null && ShopContext.hasShopContext()) {
            shopId = ShopContext.getCurrentShopId();
        }

        boolean hasVisibility = productVisibilityService.hasProductVisibility(userId, productId, shopId);
        return Result.success(hasVisibility);
    }

    /**
     * Grant visibility to user or role
     */
    @PostMapping("/grant")
    public Result<Object> grantVisibility(@RequestBody GrantVisibilityRequest request) {
        if (request.getProductIds() == null || request.getProductIds().isEmpty()) {
            return Result.error(400, "Product IDs are required");
        }
        if (request.getTargetType() == null || request.getTargetId() == null) {
            return Result.error(400, "Target type and ID are required");
        }

        // Optional: validate expiresAt format if needed, but Jackson handles ISO-8601

        productVisibilityService.grantVisibility(
                request.getProductIds(),
                request.getTargetType(),
                request.getTargetId(),
                request.getShopId(),
                request.getExpiresAt());

        return Result.success("Authorization granted successfully");
    }

    /**
     * Revoke visibility
     */
    @PostMapping("/revoke")
    public Result<Object> revokeVisibility(@RequestBody RevokeVisibilityRequest request) {
        if (request.getProductId() == null) {
            return Result.error(400, "Product ID is required");
        }
        if (request.getTargetType() == null || request.getTargetId() == null) {
            return Result.error(400, "Target type and ID are required");
        }

        productVisibilityService.revokeVisibility(
                request.getProductId(),
                request.getTargetType(),
                request.getTargetId());

        return Result.success("Authorization revoked successfully");
    }

    // DTOs defined as inner classes for simplicity, or could be separate files
    @lombok.Data
    static class GrantVisibilityRequest {
        private List<Long> productIds;
        private String targetType; // USER or ROLE
        private Long targetId;
        private Long shopId;
        private java.time.LocalDateTime expiresAt;
    }

    @lombok.Data
    static class RevokeVisibilityRequest {
        private Long productId;
        private String targetType;
        private Long targetId;
    }

    /**
     * Get authorizations for a specific product
     */
    @GetMapping("/products/{productId}/authorizations")
    public Result<List<com.logistics.track17.entity.ProductVisibility>> getProductAuthorizations(
            @PathVariable Long productId) {
        // TODO: Add permission check (e.g. only PRODUCT_MANAGER or ADMIN can view)
        List<com.logistics.track17.entity.ProductVisibility> list = productVisibilityService
                .getProductAuthorizations(productId);
        return Result.success(list);
    }
}
