package com.logistics.track17.mapper;

import com.logistics.track17.entity.DingtalkDeptMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 钉钉部门映射Mapper
 * 管理钉钉部门与系统部门的映射关系
 */
@Mapper
public interface DingtalkDeptMappingMapper {

    /**
     * 查询所有映射关系
     */
    List<DingtalkDeptMapping> selectAll();

    /**
     * 根据ID查询
     */
    DingtalkDeptMapping selectById(@Param("id") Long id);

    /**
     * 根据钉钉部门ID查询
     */
    DingtalkDeptMapping selectByDingtalkDeptId(@Param("dingtalkDeptId") Long dingtalkDeptId);

    /**
     * 根据系统部门ID查询
     */
    DingtalkDeptMapping selectBySystemDeptId(@Param("systemDeptId") Long systemDeptId);

    /**
     * 插入映射关系
     */
    int insert(DingtalkDeptMapping mapping);

    /**
     * 更新映射关系
     */
    int update(DingtalkDeptMapping mapping);

    /**
     * 删除映射关系
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据钉钉部门ID删除
     */
    int deleteByDingtalkDeptId(@Param("dingtalkDeptId") Long dingtalkDeptId);
}
