package com.logistics.track17.mapper;

import com.logistics.track17.entity.ProductVisibility;
import com.logistics.track17.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ProductVisibility Mapper Interface
 */
@Mapper
public interface ProductVisibilityMapper {

    /**
     * Insert product visibility authorization
     */
    int insert(ProductVisibility productVisibility);

    /**
     * Batch insert
     */
    int batchInsert(@Param("list") List<ProductVisibility> list);

    /**
     * Delete by ID
     */
    int deleteById(@Param("id") Long id);

    /**
     * Revoke product visibility for user
     */
    int deleteByProductAndUser(@Param("productId") Long productId, @Param("userId") Long userId);

    /**
     * Revoke product visibility for role
     */
    int deleteByProductAndRole(@Param("productId") Long productId, @Param("roleId") Long roleId);

    /**
     * Batch revoke for products
     */
    int deleteByProductIds(@Param("productIds") List<Long> productIds);

    /**
     * Select by ID
     */
    ProductVisibility selectById(@Param("id") Long id);

    /**
     * Check if user has visibility to product (direct user authorization)
     */
    int existsByUserAndProduct(@Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("shopId") Long shopId);

    /**
     * Check if any role has visibility to product
     */
    int existsByRolesAndProduct(@Param("roleIds") List<Long> roleIds,
            @Param("productId") Long productId,
            @Param("shopId") Long shopId);

    /**
     * Get authorized products for user (considering both user and role
     * authorizations)
     * 
     * @param userId  User ID
     * @param roleIds User's role IDs
     * @param shopId  Optional shop ID filter
     * @param keyword Optional search keyword
     * @param offset  Pagination offset
     * @param limit   Pagination limit
     */
    List<Product> selectAuthorizedProducts(@Param("userId") Long userId,
            @Param("roleIds") List<Long> roleIds,
            @Param("shopId") Long shopId,
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit);

    /**
     * Count authorized products
     */
    int countAuthorizedProducts(@Param("userId") Long userId,
            @Param("roleIds") List<Long> roleIds,
            @Param("shopId") Long shopId,
            @Param("keyword") String keyword);

    /**
     * Get all authorizations for a product
     */
    List<ProductVisibility> selectByProductId(@Param("productId") Long productId);

    /**
     * Get all authorizations granted by a user
     */
    List<ProductVisibility> selectByGrantedBy(@Param("grantedBy") Long grantedBy);
}
