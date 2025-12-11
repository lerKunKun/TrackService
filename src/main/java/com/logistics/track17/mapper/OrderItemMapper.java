package com.logistics.track17.mapper;

import com.logistics.track17.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 订单商品Mapper
 */
@Mapper
public interface OrderItemMapper {

    /**
     * 批量插入
     */
    void batchInsert(@Param("items") List<OrderItem> items);

    /**
     * 根据订单ID查询商品列表
     */
    List<OrderItem> selectByOrderId(Long orderId);
}
