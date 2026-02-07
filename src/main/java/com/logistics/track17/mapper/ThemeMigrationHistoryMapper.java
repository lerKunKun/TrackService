package com.logistics.track17.mapper;

import com.logistics.track17.entity.ThemeMigrationHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 主题迁移历史 Mapper
 */
@Mapper
public interface ThemeMigrationHistoryMapper {

    /**
     * 插入历史记录
     */
    int insert(ThemeMigrationHistory history);

    /**
     * 根据ID查询
     */
    ThemeMigrationHistory selectById(@Param("id") Long id);

    /**
     * 查询主题的迁移历史（按时间倒序）
     */
    List<ThemeMigrationHistory> selectByThemeName(@Param("themeName") String themeName);

    /**
     * 查询指定状态的迁移记录
     */
    List<ThemeMigrationHistory> selectByStatus(@Param("status") String status);

    /**
     * 更新状态
     */
    int updateStatus(@Param("id") Long id,
            @Param("status") String status,
            @Param("errorMessage") String errorMessage);

    /**
     * 更新完成信息
     */
    int updateCompletion(ThemeMigrationHistory history);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);
}
