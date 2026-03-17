package com.logistics.track17.mapper;

import com.logistics.track17.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductImageMapper {

    /**
     * 批量插入产品图片
     */
    int batchInsert(@Param("images") List<ProductImage> images);

    /**
     * 批量查询多个产品的图片（按 product_id, position 排序）
     */
    List<ProductImage> selectByProductIds(@Param("productIds") List<Long> productIds);

    /**
     * 根据产品ID删除所有图片
     */
    int deleteByProductId(@Param("productId") Long productId);
}
