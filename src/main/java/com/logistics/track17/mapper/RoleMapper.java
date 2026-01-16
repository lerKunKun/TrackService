package com.logistics.track17.mapper;

import com.logistics.track17.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色Mapper接口
 */
@Mapper
public interface RoleMapper {

    /**
     * 查询所有角色
     */
    List<Role> selectAll();

    /**
     * 根据ID查询角色
     */
    Role selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询角色
     */
    List<Role> selectByUserId(@Param("userId") Long userId);

    /**
     * 插入角色
     */
    int insert(Role role);

    /**
     * 更新角色
     */
    int update(Role role);

    /**
     * 删除角色
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据角色编码查询
     */
    Role selectByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 为角色分配菜单
     */
    int assignMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    /**
     * 删除角色的所有菜单
     */
    int deleteMenusByRoleId(@Param("roleId") Long roleId);

    /**
     * 为角色分配权限
     */
    int assignPermissions(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    /**
     * 删除角色的所有权限
     */
    int deletePermissionsByRoleId(@Param("roleId") Long roleId);

    /**
     * 为用户分配角色
     */
    int assignRolesToUser(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 删除用户的所有角色
     */
    int deleteRolesByUserId(@Param("userId") Long userId);

    /**
     * 批量删除用户的角色
     */
    void deleteRolesByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 批量为用户分配角色
     */
    void batchAssignRolesToUsers(@Param("assignments") List<Map<String, Long>> assignments);
}
