package com.logistics.track17.mapper;

import com.logistics.track17.entity.Parcel;
import org.apache.ibatis.annotations.Param;

/**
 * 包裹Mapper
 */
public interface ParcelMapper {

    /**
     * 插入包裹
     */
    int insert(Parcel parcel);

    /**
     * 根据ID查询包裹
     */
    Parcel selectById(@Param("id") Long id);

    /**
     * 根据订单ID查询包裹
     */
    Parcel selectByOrderId(@Param("orderId") Long orderId);
}
