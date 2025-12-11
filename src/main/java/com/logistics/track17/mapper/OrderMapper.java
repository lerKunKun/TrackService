package com.logistics.track17.mapper;

import com.logistics.track17.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper {

    /**
     * 插入订单
     */
    void insert(Order order);

    /**
     * 根据ID查询
     */
    Order selectById(Long id);

    /**
     * 根据ShopifyOrderId查询
     */
    Order selectByShopifyOrderId(@Param("shopId") Long shopId, @Param("shopifyOrderId") Long shopifyOrderId);

    /**
     * 查询订单列表
     */
    List<Order> selectList(@Param("shopId") Long shopId,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit);

    /**
     * 统计订单数量
     */
    Long count(@Param("shopId") Long shopId);

    /**
     * 更新订单
     */
    void update(Order order);
}
