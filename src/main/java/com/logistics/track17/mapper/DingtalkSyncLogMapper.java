package com.logistics.track17.mapper;

import com.logistics.track17.entity.DingtalkSyncLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 钉钉同步日志Mapper
 * 记录每次同步操作的详细信息
 */
@Mapper
public interface DingtalkSyncLogMapper {

    /**
     * 查询所有同步日志
     */
    List<DingtalkSyncLog> selectAll();

    /**
     * 查询最近N条同步日志
     */
    List<DingtalkSyncLog> selectRecent(@Param("limit") Integer limit);

    /**
     * 根据ID查询
     */
    DingtalkSyncLog selectById(@Param("id") Long id);

    /**
     * 根据同步类型查询
     */
    List<DingtalkSyncLog> selectBySyncType(@Param("syncType") String syncType);

    /**
     * 插入同步日志
     */
    int insert(DingtalkSyncLog log);

    /**
     * 更新同步日志
     */
    int update(DingtalkSyncLog log);
}
