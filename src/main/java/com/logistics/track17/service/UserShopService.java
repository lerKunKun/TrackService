package com.logistics.track17.service;

import com.logistics.track17.entity.Role;
import com.logistics.track17.entity.Shop;
import com.logistics.track17.entity.UserShopRole;
import com.logistics.track17.mapper.ShopMapper;
import com.logistics.track17.mapper.UserShopRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User Shop Service
 * Manages user-shop-role associations and shop access permissions
 */
@Service
@Slf4j
public class UserShopService {

    @Autowired
    private UserShopRoleMapper userShopRoleMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private RoleService roleService;

    /**
     * Check if user has access to shop
     * Super admins and global admins have access to all shops
     * 
     * @param userId User ID
     * @param shopId Shop ID
     * @return true if user can access shop
     */
    @Cacheable(value = "user:shop:access", key = "#userId + ':' + #shopId")
    public boolean hasShopAccess(Long userId, Long shopId) {
        if (userId == null || shopId == null) {
            return false;
        }

        // Global admins have access to all shops
        if (isGlobalAdmin(userId)) {
            return true;
        }

        // Check user_shop_roles table
        int count = userShopRoleMapper.countByUserAndShop(userId, shopId);
        return count > 0;
    }

    /**
     * Get all shops accessible by user
     * 
     * @param userId User ID
     * @return List of accessible shops
     */
    @Cacheable(value = "user:shops", key = "#userId")
    public List<Shop> getUserAccessibleShops(Long userId) {
        if (userId == null) {
            return List.of();
        }

        // Global admins see all shops
        if (isGlobalAdmin(userId)) {
            return shopMapper.selectAll();
        }

        // Return shops from user_shop_roles
        return userShopRoleMapper.selectShopsByUserId(userId);
    }

    /**
     * Grant shop access to user with specific role
     * 
     * @param userId    User ID
     * @param shopId    Shop ID
     * @param roleId    Role ID
     * @param grantedBy Who grants this permission
     * @param expiresAt Expiration time (null for permanent)
     */
    @Transactional
    public void grantShopAccess(Long userId, Long shopId, Long roleId,
            Long grantedBy, LocalDateTime expiresAt) {
        UserShopRole usr = new UserShopRole();
        usr.setUserId(userId);
        usr.setShopId(shopId);
        usr.setRoleId(roleId);
        usr.setGrantedBy(grantedBy);
        usr.setGrantedAt(LocalDateTime.now());
        usr.setExpiresAt(expiresAt);

        userShopRoleMapper.insert(usr);
        log.info("Granted shop access: userId={}, shopId={}, roleId={}", userId, shopId, roleId);
    }

    /**
     * Revoke shop access from user
     * 
     * @param userId User ID
     * @param shopId Shop ID
     * @param roleId Role ID
     */
    @Transactional
    public void revokeShopAccess(Long userId, Long shopId, Long roleId) {
        userShopRoleMapper.deleteByUserShopRole(userId, shopId, roleId);
        log.info("Revoked shop access: userId={}, shopId={}, roleId={}", userId, shopId, roleId);
    }

    /**
     * Get user's role IDs in a specific shop
     * 
     * @param userId User ID
     * @param shopId Shop ID
     * @return List of role IDs
     */
    public List<Long> getUserRoleIdsInShop(Long userId, Long shopId) {
        if (userId == null || shopId == null) {
            return List.of();
        }

        return userShopRoleMapper.selectRoleIdsByUserAndShop(userId, shopId);
    }

    /**
     * Get all user-shop-role associations for a user
     * 
     * @param userId User ID
     * @return List of UserShopRole
     */
    public List<UserShopRole> getUserShopRoles(Long userId) {
        if (userId == null) {
            return List.of();
        }

        return userShopRoleMapper.selectByUserId(userId);
    }

    /**
     * Check if user is global admin (SUPER_ADMIN or ADMIN)
     * 
     * @param userId User ID
     * @return true if user is global admin
     */
    private boolean isGlobalAdmin(Long userId) {
        List<Role> roles = roleService.getRolesByUserId(userId);
        return roles.stream()
                .map(Role::getRoleCode)
                .anyMatch(code -> "SUPER_ADMIN".equals(code) || "ADMIN".equals(code));
    }
}
