package com.logistics.track17.controller;

import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.UserShopRole;
import com.logistics.track17.service.UserShopService;
import com.logistics.track17.util.UserContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Shop Controller
 * Managing user's access to shops
 */
@RestController
@RequestMapping("/user-shops")
public class UserShopController {

    @Autowired
    private UserShopService userShopService;

    /**
     * Get shops accessible by current user
     */
    @GetMapping("/list")
    public Result<Object> getMyShops() {
        Long userId = UserContextHolder.getCurrentUserId();
        return Result.success(userShopService.getUserAccessibleShops(userId));
    }

    /**
     * Get user's roles in a specific shop
     */
    @GetMapping("/{shopId}/roles")
    public Result<Object> getMyRolesInShop(@PathVariable Long shopId) {
        Long userId = UserContextHolder.getCurrentUserId();
        return Result.success(userShopService.getUserRoleIdsInShop(userId, shopId));
    }

    /**
     * Check if user has access to a shop
     */
    @GetMapping("/{shopId}/check-access")
    public Result<Object> checkShopAccess(@PathVariable Long shopId) {
        Long userId = UserContextHolder.getCurrentUserId();
        boolean hasAccess = userShopService.hasShopAccess(userId, shopId);
        return Result.success(hasAccess);
    }
}
