package com.logistics.track17.service;

import com.logistics.track17.entity.Product;
import com.logistics.track17.entity.ProductVisibility;
import com.logistics.track17.entity.Role;
import com.logistics.track17.mapper.ProductVisibilityMapper;
import com.logistics.track17.util.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Product Visibility Service
 * Manages product authorization - who can view and publish which products
 */
@Service
@Slf4j
public class ProductVisibilityService {

    @Autowired
    private ProductVisibilityMapper productVisibilityMapper;

    @Autowired
    private RoleService roleService;

    @Autowired
    private com.logistics.track17.mapper.ProductMapper productMapper;

    /**
     * Grant product visibility to user or role
     * 
     * @param productIds List of product IDs
     * @param targetType "USER" or "ROLE"
     * @param targetId   User ID or Role ID
     * @param shopId     Optional shop ID (null = all shops)
     * @param expiresAt  Optional expiration time
     */
    @Transactional
    public void grantVisibility(List<Long> productIds, String targetType,
            Long targetId, Long shopId, LocalDateTime expiresAt) {
        Long currentUserId = UserContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("User not authenticated");
        }

        for (Long productId : productIds) {
            ProductVisibility pv = new ProductVisibility();
            pv.setProductId(productId);

            if ("USER".equalsIgnoreCase(targetType)) {
                pv.setUserId(targetId);
                pv.setRoleId(null);
            } else if ("ROLE".equalsIgnoreCase(targetType)) {
                pv.setUserId(null);
                pv.setRoleId(targetId);
            } else {
                throw new IllegalArgumentException("Invalid target type: " + targetType);
            }

            pv.setShopId(shopId);
            pv.setGrantedBy(currentUserId);
            pv.setGrantedAt(LocalDateTime.now());
            pv.setExpiresAt(expiresAt);

            productVisibilityMapper.insert(pv);
        }

        log.info("Granted product visibility: productIds={}, targetType={}, targetId={}, grantedBy={}",
                productIds, targetType, targetId, currentUserId);
    }

    /**
     * Batch grant visibility (optimized for large operations)
     */
    @Transactional
    public void batchGrantVisibility(List<ProductVisibility> visibilities) {
        Long currentUserId = UserContextHolder.getCurrentUserId();

        for (ProductVisibility pv : visibilities) {
            pv.setGrantedBy(currentUserId);
            pv.setGrantedAt(LocalDateTime.now());
        }

        productVisibilityMapper.batchInsert(visibilities);
        log.info("Batch granted visibility for {} products", visibilities.size());
    }

    /**
     * Revoke product visibility
     * 
     * @param productId  Product ID
     * @param targetType "USER" or "ROLE"
     * @param targetId   User ID or Role ID
     */
    @Transactional
    public void revokeVisibility(Long productId, String targetType, Long targetId) {
        if ("USER".equalsIgnoreCase(targetType)) {
            productVisibilityMapper.deleteByProductAndUser(productId, targetId);
        } else if ("ROLE".equalsIgnoreCase(targetType)) {
            productVisibilityMapper.deleteByProductAndRole(productId, targetId);
        } else {
            throw new IllegalArgumentException("Invalid target type: " + targetType);
        }

        log.info("Revoked product visibility: productId={}, targetType={}, targetId={}",
                productId, targetType, targetId);
    }

    /**
     * Check if user has visibility to product
     * Global admins automatically have access to all products
     * 
     * @param userId    User ID
     * @param productId Product ID
     * @param shopId    Optional shop ID
     * @return true if user can view/publish this product
     */
    public boolean hasProductVisibility(Long userId, Long productId, Long shopId) {
        if (userId == null || productId == null) {
            return false;
        }

        // Global admins have access to all products
        if (isGlobalAdmin(userId)) {
            return true;
        }

        // Check direct user authorization
        int userAuth = productVisibilityMapper.existsByUserAndProduct(userId, productId, shopId);
        if (userAuth > 0) {
            return true;
        }

        // Check role-based authorization
        List<Long> roleIds = getRoleIds(userId);
        if (!roleIds.isEmpty()) {
            int roleAuth = productVisibilityMapper.existsByRolesAndProduct(roleIds, productId, shopId);
            if (roleAuth > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get authorized products for user
     * 
     * @param userId  User ID
     * @param shopId  Optional shop ID filter
     * @param keyword Optional search keyword
     * @param page    Page number (1-indexed)
     * @param size    Page size
     * @return List of authorized products
     */
    public List<Product> getAuthorizedProducts(Long userId, Long shopId, String keyword,
            int page, int size) {
        if (userId == null) {
            return List.of();
        }

        // Global admins have access to all products
        if (isGlobalAdmin(userId)) {
            int offset = (page - 1) * size;
            // Use ProductMapper directly to bypass visibility join
            return productMapper.selectByPage(keyword, null, null, shopId, offset, size);
        }

        List<Long> roleIds = getRoleIds(userId);
        int offset = (page - 1) * size;

        return productVisibilityMapper.selectAuthorizedProducts(
                userId, roleIds, shopId, keyword, offset, size);
    }

    /**
     * Count authorized products
     */
    public int countAuthorizedProducts(Long userId, Long shopId, String keyword) {
        if (userId == null) {
            return 0;
        }

        // Global admins have access to all products
        if (isGlobalAdmin(userId)) {
            return productMapper.countByFilters(keyword, null, null, shopId);
        }

        List<Long> roleIds = getRoleIds(userId);
        return productVisibilityMapper.countAuthorizedProducts(userId, roleIds, shopId, keyword);
    }

    /**
     * Get all authorizations for a product (for management UI)
     */
    public List<ProductVisibility> getProductAuthorizations(Long productId) {
        return productVisibilityMapper.selectByProductId(productId);
    }

    /**
     * Get role IDs for user
     */
    private List<Long> getRoleIds(Long userId) {
        List<Role> roles = roleService.getRolesByUserId(userId);
        return roles.stream()
                .map(Role::getId)
                .toList();
    }

    /**
     * Check if user is global admin
     */
    private boolean isGlobalAdmin(Long userId) {
        List<Role> roles = roleService.getRolesByUserId(userId);
        return roles.stream()
                .map(Role::getRoleCode)
                .anyMatch(code -> "SUPER_ADMIN".equals(code) || "ADMIN".equals(code));
    }
}
