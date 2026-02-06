package com.logistics.track17.mapper;

import com.logistics.track17.entity.ProductShop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
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
         * @param productId     产品ID
         * @param shopIds       商店ID列表
         * @param publishedBy   刊登人
         * @param publishStatus 刊登状态
         * @return 影响行数
         */
        int batchInsert(@Param("productId") Long productId,
                        @Param("shopIds") List<Long> shopIds,
                        @Param("publishedBy") Long publishedBy,
                        @Param("publishStatus") Integer publishStatus);

        /**
         * 批量插入关联列表 (支持多产品多店铺)
         * 
         * @param list 关联对象列表
         * @return 影响行数
         */
        int batchInsertList(@Param("list") List<ProductShop> list);

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
         * 根据产品ID列表批量查询商店关联
         * 
         * @param productIds 产品ID列表
         * @return 关联列表
         */
        List<ProductShop> selectByProductIds(@Param("productIds") List<Long> productIds);

        /**
         * 删除产品的所有商店关联
         * 
         * @param productId 产品ID
         * @return 影响行数
         */
        int deleteByProductId(@Param("productId") Long productId);

        /**
         * 批量删除产品的商店关联
         * 
         * @param productIds 产品ID列表
         * @return 影响行数
         */
        int deleteByProductIds(@Param("productIds") List<Long> productIds);

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

        /**
         * 更新刊登导出状态
         */
        int updatePublishStatus(@Param("productId") Long productId,
                        @Param("shopId") Long shopId,
                        @Param("status") Integer status,
                        @Param("lastExportTime") LocalDateTime lastExportTime,
                        @Param("publishedBy") Long publishedBy);
}
