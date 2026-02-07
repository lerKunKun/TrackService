package com.logistics.track17.mapper;

import com.logistics.track17.entity.LiquidSchemaCache;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Liquid Schema缓存Mapper
 */
@Mapper
public interface LiquidSchemaCacheMapper {

    /**
     * 插入schema缓存
     */
    int insert(LiquidSchemaCache cache);

    /**
     * 批量插入
     */
    int batchInsert(@Param("list") List<LiquidSchemaCache> cacheList);

    /**
     * 根据ID查询
     */
    LiquidSchemaCache selectById(@Param("id") Long id);

    /**
     * 根据主题和版本查询所有schema
     */
    List<LiquidSchemaCache> selectByThemeVersion(
            @Param("themeName") String themeName,
            @Param("version") String version);

    /**
     * 根据主题、版本和文件路径查询
     */
    LiquidSchemaCache selectByFile(
            @Param("themeName") String themeName,
            @Param("version") String version,
            @Param("filePath") String filePath);

    /**
     * 根据section名称查询
     */
    List<LiquidSchemaCache> selectBySectionName(
            @Param("themeName") String themeName,
            @Param("version") String version,
            @Param("sectionName") String sectionName);

    /**
     * 更新schema缓存
     */
    int update(LiquidSchemaCache cache);

    /**
     * 删除指定主题版本的所有缓存
     */
    int deleteByThemeVersion(
            @Param("themeName") String themeName,
            @Param("version") String version);

    /**
     * 根据ID删除
     */
    int deleteById(@Param("id") Long id);
}
