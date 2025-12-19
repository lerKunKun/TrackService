package com.logistics.track17.mapper;

import com.logistics.track17.entity.ThemeVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 主题版本存档 Mapper
 */
@Mapper
public interface ThemeVersionMapper {

    /**
     * 插入主题版本
     */
    int insert(ThemeVersion themeVersion);

    /**
     * 根据ID查询
     */
    ThemeVersion selectById(@Param("id") Long id);

    /**
     * 根据主题名称和版本号查询
     */
    ThemeVersion selectByThemeAndVersion(@Param("themeName") String themeName,
            @Param("version") String version);

    /**
     * 获取当前版本
     */
    ThemeVersion selectCurrentVersion(@Param("themeName") String themeName);

    /**
     * 获取主题的所有版本（按时间倒序）
     */
    List<ThemeVersion> selectByThemeName(@Param("themeName") String themeName);

    /**
     * 更新
     */
    int update(ThemeVersion themeVersion);

    /**
     * 清除主题的current标记
     */
    int clearCurrentFlag(@Param("themeName") String themeName);

    /**
     * 设置为当前版本
     */
    int setAsCurrentVersion(@Param("themeName") String themeName,
            @Param("version") String version);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);
}
