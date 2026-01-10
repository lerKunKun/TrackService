package com.logistics.track17.service;

import com.logistics.track17.entity.Role;
import com.logistics.track17.mapper.RoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
     * 使用缓存提升性能，缓存 30 分钟
     */
    @Cacheable(value = "user:roles", key = "#userId")
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
     * 分配后不需要清除权限缓存（菜单和权限独立）
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
     * 分配后清除所有用户的权限缓存（因为角色权限变更会影响用户权限）
     */
    @Transactional
    @CacheEvict(value = { "user:permissions", "user:permissions:codes" }, allEntries = true)
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
     * 注意：允许传入空列表清空所有角色（用于离职员工、临时禁用等场景）
     * 分配后清除该用户的角色和权限缓存
     */
    @Transactional
    @CacheEvict(value = { "user:roles", "user:permissions", "user:permissions:codes" }, key = "#userId")
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        // 先删除旧的关联
        roleMapper.deleteRolesByUserId(userId);

        // 如果 roleIds 为空或null，表示清空所有角色（允许此操作）
        if (roleIds != null && !roleIds.isEmpty()) {
            roleMapper.assignRolesToUser(userId, roleIds);
        }

        log.info("为用户 {} 分配角色成功，角色数: {}", userId, roleIds != null ? roleIds.size() : 0);
    }
}
