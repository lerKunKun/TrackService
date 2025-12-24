package com.logistics.track17.mapper;

import com.logistics.track17.entity.ProductShop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 产品商店关联数据访问层
 */
@Mapper
public interface ProductShopMapper {

    /**
     * 插入产品商店关联
     * 
     * @param productShop 关联对象
     * @return 影响行数
     */
    int insert(ProductShop productShop);

    /**
     * 批量插入关联关系
     * 
     * @param productId 产品ID
     * @param shopIds   商店ID列表
     * @return 影响行数
     */
    int batchInsert(@Param("productId") Long productId, @Param("shopIds") List<Long> shopIds);

    /**
     * 根据产品ID查询关联的所有商店ID
     * 
     * @param productId 产品ID
     * @return 商店ID列表
     */
    List<Long> selectShopIdsByProductId(@Param("productId") Long productId);

    /**
     * 根据商店ID查询关联的所有产品ID
     * 
     * @param shopId 商店ID
     * @return 产品ID列表
     */
    List<Long> selectProductIdsByShopId(@Param("shopId") Long shopId);

    /**
     * 删除产品的所有商店关联
     * 
     * @param productId 产品ID
     * @return 影响行数
     */
    int deleteByProductId(@Param("productId") Long productId);

    /**
     * 删除商店的所有产品关联
     * 
     * @param shopId 商店ID
     * @return 影响行数
     */
    int deleteByShopId(@Param("shopId") Long shopId);

    /**
     * 删除指定的产品商店关联
     * 
     * @param productId 产品ID
     * @param shopId    商店ID
     * @return 影响行数
     */
    int deleteByProductIdAndShopId(@Param("productId") Long productId, @Param("shopId") Long shopId);
}
