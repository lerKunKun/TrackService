package com.logistics.track17.mapper;

import com.logistics.track17.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限Mapper接口
 */
@Mapper
public interface PermissionMapper {

    /**
     * 查询所有权限
     */
    List<Permission> selectAll();

    /**
     * 根据ID查询权限
     */
    Permission selectById(@Param("id") Long id);

    /**
     * 根据角色ID查询权限
     */
    List<Permission> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询权限（通过用户角色）
     */
    List<Permission> selectByUserId(@Param("userId") Long userId);

    /**
     * 插入权限
     */
    int insert(Permission permission);

    /**
     * 更新权限
     */
    int update(Permission permission);

    /**
     * 删除权限
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据权限编码查询
     */
    Permission selectByPermissionCode(@Param("permissionCode") String permissionCode);
}
