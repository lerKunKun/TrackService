package com.logistics.track17.mapper;

import com.logistics.track17.entity.ThemeExport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 主题导出记录 Mapper
 */
@Mapper
public interface ThemeExportMapper {

    /**
     * 插入导出记录
     */
    int insert(ThemeExport export);

    /**
     * 根据ID查询
     */
    ThemeExport selectById(@Param("id") Long id);

    /**
     * 根据迁移ID查询
     */
    ThemeExport selectByMigrationId(@Param("migrationId") Long migrationId);

    /**
     * 查询导出历史
     */
    List<ThemeExport> selectHistory(@Param("themeName") String themeName);

    /**
     * 增加下载次数
     */
    int incrementDownloadCount(@Param("id") Long id);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);
}
