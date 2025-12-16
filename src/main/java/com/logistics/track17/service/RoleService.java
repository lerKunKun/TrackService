package com.logistics.track17.service;

import com.logistics.track17.entity.Role;
import com.logistics.track17.mapper.RoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务
 */
@Service
@Slf4j
public class RoleService {

    @Autowired
    private RoleMapper roleMapper;

    /**
     * 获取所有角色
     */
    public List<Role> getAllRoles() {
        return roleMapper.selectAll();
    }

    /**
     * 根据ID获取角色
     */
    public Role getRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    /**
     * 根据用户ID获取角色
     */
    public List<Role> getRolesByUserId(Long userId) {
        return roleMapper.selectByUserId(userId);
    }

    /**
     * 创建角色
     */
    public Role createRole(Role role) {
        // 检查角色编码是否已存在
        Role existing = roleMapper.selectByRoleCode(role.getRoleCode());
        if (existing != null) {
            throw new RuntimeException("角色编码已存在: " + role.getRoleCode());
        }

        roleMapper.insert(role);
        log.info("创建角色成功: {}", role.getRoleName());
        return role;
    }

    /**
     * 更新角色
     */
    public Role updateRole(Role role) {
        Role existing = roleMapper.selectById(role.getId());
        if (existing == null) {
            throw new RuntimeException("角色不存在: " + role.getId());
        }

        // 超级管理员不允许修改
        if ("SUPER_ADMIN".equals(existing.getRoleCode())) {
            throw new RuntimeException("超级管理员角色不允许修改");
        }

        roleMapper.update(role);
        log.info("更新角色成功: {}", role.getRoleName());
        return role;
    }

    /**
     * 删除角色
     */
    @Transactional
    public void deleteRole(Long id) {
        Role existing = roleMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("角色不存在: " + id);
        }

        // 超级管理员不允许删除
        if ("SUPER_ADMIN".equals(existing.getRoleCode())) {
            throw new RuntimeException("超级管理员角色不允许删除");
        }

        // 删除角色关联的菜单和权限
        roleMapper.deleteMenusByRoleId(id);
        roleMapper.deletePermissionsByRoleId(id);
        roleMapper.deleteById(id);

        log.info("删除角色成功: {}", id);
    }

    /**
     * 为角色分配菜单
     */
    @Transactional
    public void assignMenusToRole(Long roleId, List<Long> menuIds) {
        // 先删除旧的关联
        roleMapper.deleteMenusByRoleId(roleId);

        // 再插入新的关联
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMapper.assignMenus(roleId, menuIds);
        }

        log.info("为角色 {} 分配菜单成功，菜单数: {}", roleId, menuIds != null ? menuIds.size() : 0);
    }

    /**
     * 为角色分配权限
     */
    @Transactional
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        // 先删除旧的关联
        roleMapper.deletePermissionsByRoleId(roleId);

        // 再插入新的关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            roleMapper.assignPermissions(roleId, permissionIds);
        }

        log.info("为角色 {} 分配权限成功，权限数: {}", roleId, permissionIds != null ? permissionIds.size() : 0);
    }

    /**
     * 为用户分配角色
     */
    @Transactional
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new RuntimeException("至少需要分配一个角色");
        }

        // 先删除旧的关联
        roleMapper.deleteRolesByUserId(userId);

        // 再插入新的关联
        roleMapper.assignRolesToUser(userId, roleIds);

        log.info("为用户 {} 分配角色成功，角色数: {}", userId, roleIds.size());
    }
}
