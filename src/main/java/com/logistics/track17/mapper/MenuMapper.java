package com.logistics.track17.mapper;

import com.logistics.track17.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单Mapper接口
 */
@Mapper
public interface MenuMapper {

    /**
     * 查询所有菜单
     */
    List<Menu> selectAll();

    /**
     * 根据ID查询菜单
     */
    Menu selectById(@Param("id") Long id);

    /**
     * 根据父ID查询子菜单
     */
    List<Menu> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 根据角色ID查询菜单
     */
    List<Menu> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询菜单（通过用户角色）
     */
    List<Menu> selectByUserId(@Param("userId") Long userId);

    /**
     * 插入菜单
     */
    int insert(Menu menu);

    /**
     * 更新菜单
     */
    int update(Menu menu);

    /**
     * 删除菜单
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据菜单编码查询
     */
    Menu selectByMenuCode(@Param("menuCode") String menuCode);
}
