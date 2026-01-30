package com.logistics.track17.mapper;

import com.logistics.track17.entity.ProductVariant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 产品变体数据访问层
 */
@Mapper
public interface ProductVariantMapper {

    /**
     * 插入产品变体
     * 
     * @param variant 变体对象
     * @return 影响行数
     */
    int insert(ProductVariant variant);

    /**
     * 批量插入产品变体
     * 
     * @param variants 变体列表
     * @return 影响行数
     */
    int batchInsert(@Param("variants") List<ProductVariant> variants);

    /**
     * 根据ID查询变体
     * 
     * @param id 变体ID
     * @return 变体对象
     */
    ProductVariant selectById(@Param("id") Long id);

    /**
     * 根据产品ID查询所有变体
     * 
     * @param productId 产品ID
     * @return 变体列表
     */
    List<ProductVariant> selectByProductId(@Param("productId") Long productId);

    /**
     * 根据产品ID查询第一个变体
     * 
     * @param productId 产品ID
     * @return 第一个变体对象
     */
    ProductVariant selectFirstByProductId(@Param("productId") Long productId);

    /**
     * 批量查询产品的第一个变体 (用于列表展示)
     * 
     * @param productIds 产品ID列表
     * @return 变体列表
     */
    List<ProductVariant> selectFirstVariantsByProductIds(@Param("productIds") List<Long> productIds);

    /**
     * 更新变体
     * 
     * @param variant 变体对象
     * @return 影响行数
     */
    int update(ProductVariant variant);

    /**
     * 删除变体
     * 
     * @param id 变体ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 删除产品的所有变体
     * 
     * @param productId 产品ID
     * @return 影响行数
     */
    int deleteByProductId(@Param("productId") Long productId);

    /**
     * 根据SKU查询变体
     * 
     * @param sku SKU
     * @return 变体对象
     */
    ProductVariant selectBySku(@Param("sku") String sku);

    /**
     * 更新变体价格和原价
     * 
     * @param id             变体ID
     * @param price          销售价格
     * @param compareAtPrice 原价（对比价格）
     * @return 影响行数
     */
    int updatePrice(@Param("id") Long id,
            @Param("price") java.math.BigDecimal price,
            @Param("compareAtPrice") java.math.BigDecimal compareAtPrice);
}
