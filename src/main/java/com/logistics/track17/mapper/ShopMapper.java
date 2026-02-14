package com.logistics.track17.mapper;

import com.logistics.track17.entity.Shop;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 店铺Mapper
 */
public interface ShopMapper {

    /**
     * 插入店铺
     */
    int insert(Shop shop);

    /**
     * 查询所有店铺
     */
    List<Shop> selectAll();

    /**
     * 根据ID查询店铺
     */
    Shop selectById(@Param("id") Long id);

    /**
     * 根据店铺域名查询
     */
    Shop selectByShopDomain(@Param("shopDomain") String shopDomain);

    /**
     * 查询店铺列表
     */
    List<Shop> selectList(@Param("platform") String platform,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 统计店铺数量
     */
    Long count(@Param("platform") String platform);

    /**
     * 更新店铺
     */
    int update(Shop shop);

    /**
     * 删除店铺
     */
    int deleteById(@Param("id") Long id);

    /**
     * 统计店铺关联的订单数量
     */
    Long countOrdersByShopId(@Param("shopId") Long shopId);

    /**
     * 按活跃状态查询店铺
     */
    List<Shop> findByIsActive(@Param("isActive") boolean isActive);

}
