package com.logistics.track17.mapper;

import com.logistics.track17.entity.TrackingEvent;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 物流事件Mapper
 */
public interface TrackingEventMapper {

    /**
     * 插入事件
     */
    int insert(TrackingEvent event);

    /**
     * 批量插入事件
     */
    int insertBatch(@Param("events") List<TrackingEvent> events);

    /**
     * 根据运单ID查询事件列表
     */
    List<TrackingEvent> selectByTrackingId(@Param("trackingId") Long trackingId);

    /**
     * 删除运单的所有事件
     */
    int deleteByTrackingId(@Param("trackingId") Long trackingId);
}
