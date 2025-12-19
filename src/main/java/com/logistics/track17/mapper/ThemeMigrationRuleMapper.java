package com.logistics.track17.mapper;

import com.logistics.track17.entity.ThemeMigrationRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 主题迁移规则Mapper
 */
@Mapper
public interface ThemeMigrationRuleMapper {

    /**
     * 插入规则
     */
    int insert(ThemeMigrationRule rule);

    /**
     * 批量插入规则
     */
    int batchInsert(@Param("list") List<ThemeMigrationRule> rules);

    /**
     * 根据ID查询
     */
    ThemeMigrationRule selectById(@Param("id") Long id);

    /**
     * 根据版本查询所有规则
     */
    List<ThemeMigrationRule> selectByVersions(
            @Param("themeName") String themeName,
            @Param("fromVersion") String fromVersion,
            @Param("toVersion") String toVersion);

    /**
     * 根据版本和规则类型查询
     */
    List<ThemeMigrationRule> selectByVersionsAndType(
            @Param("themeName") String themeName,
            @Param("fromVersion") String fromVersion,
            @Param("toVersion") String toVersion,
            @Param("ruleType") String ruleType);

    /**
     * 根据版本、类型和section查询
     */
    List<ThemeMigrationRule> selectByVersionsTypeAndSection(
            @Param("themeName") String themeName,
            @Param("fromVersion") String fromVersion,
            @Param("toVersion") String toVersion,
            @Param("ruleType") String ruleType,
            @Param("sectionName") String sectionName);

    /**
     * 更新规则
     */
    int update(ThemeMigrationRule rule);

    /**
     * 删除指定版本的所有规则
     */
    int deleteByVersions(
            @Param("themeName") String themeName,
            @Param("fromVersion") String fromVersion,
            @Param("toVersion") String toVersion);

    /**
     * 根据ID删除
     */
    int deleteById(@Param("id") Long id);
}
