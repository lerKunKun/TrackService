package com.logistics.track17.mapper;

import com.logistics.track17.entity.UserShopRole;
import com.logistics.track17.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * UserShopRole Mapper Interface
 */
@Mapper
public interface UserShopRoleMapper {

    /**
     * Insert user-shop-role association
     */
    int insert(UserShopRole userShopRole);

    /**
     * Batch insert user-shop-role associations
     */
    int batchInsert(@Param("list") List<UserShopRole> list);

    /**
     * Delete by ID
     */
    int deleteById(@Param("id") Long id);

    /**
     * Delete user's role in a shop
     */
    int deleteByUserShopRole(@Param("userId") Long userId,
            @Param("shopId") Long shopId,
            @Param("roleId") Long roleId);

    /**
     * Select by ID
     */
    UserShopRole selectById(@Param("id") Long id);

    /**
     * Check if user has access to shop
     */
    int countByUserAndShop(@Param("userId") Long userId, @Param("shopId") Long shopId);

    /**
     * Get user's shops (returns Shop entities)
     */
    List<Shop> selectShopsByUserId(@Param("userId") Long userId);

    /**
     * Get user-shop-role associations for a user
     */
    List<UserShopRole> selectByUserId(@Param("userId") Long userId);

    /**
     * Get user-shop-role associations for a shop
     */
    List<UserShopRole> selectByShopId(@Param("shopId") Long shopId);

    /**
     * Get all role IDs for a user
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * Get role IDs for a user in a specific shop
     */
    List<Long> selectRoleIdsByUserAndShop(@Param("userId") Long userId, @Param("shopId") Long shopId);
}
