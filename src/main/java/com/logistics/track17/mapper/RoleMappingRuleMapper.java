package com.logistics.track17.mapper;

import com.logistics.track17.entity.RoleMappingRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色映射规则Mapper
 * 管理钉钉部门/职位到系统角色的自动分配规则
 */
@Mapper
public interface RoleMappingRuleMapper {

    /**
     * 查询所有映射规则
     */
    List<RoleMappingRule> selectAll();

    /**
     * 查询所有启用的映射规则
     */
    List<RoleMappingRule> selectAllEnabled();

    /**
     * 根据ID查询
     */
    RoleMappingRule selectById(@Param("id") Long id);

    /**
     * 根据规则类型查询
     */
    List<RoleMappingRule> selectByRuleType(@Param("ruleType") String ruleType);

    /**
     * 插入映射规则
     */
    int insert(RoleMappingRule rule);

    /**
     * 更新映射规则
     */
    int update(RoleMappingRule rule);

    /**
     * 删除映射规则
     */
    int deleteById(@Param("id") Long id);

    /**
     * 更新规则状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
