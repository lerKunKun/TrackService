package com.logistics.track17.service;

import com.logistics.track17.entity.Permission;
import com.logistics.track17.mapper.PermissionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限服务
 */
@Service
@Slf4j
public class PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    /**
     * 获取所有权限
     */
    public List<Permission> getAllPermissions() {
        return permissionMapper.selectAll();
    }

    /**
     * 根据ID获取权限
     */
    public Permission getPermissionById(Long id) {
        return permissionMapper.selectById(id);
    }

    /**
     * 根据角色ID获取权限
     */
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return permissionMapper.selectByRoleId(roleId);
    }

    /**
     * 根据用户ID获取权限
     */
    public List<Permission> getPermissionsByUserId(Long userId) {
        return permissionMapper.selectByUserId(userId);
    }

    /**
     * 创建权限
     */
    public Permission createPermission(Permission permission) {
        // 检查权限编码是否已存在
        Permission existing = permissionMapper.selectByPermissionCode(permission.getPermissionCode());
        if (existing != null) {
            throw new RuntimeException("权限编码已存在: " + permission.getPermissionCode());
        }

        permissionMapper.insert(permission);
        log.info("创建权限成功: {}", permission.getPermissionName());
        return permission;
    }

    /**
     * 更新权限
     */
    public Permission updatePermission(Permission permission) {
        Permission existing = permissionMapper.selectById(permission.getId());
        if (existing == null) {
            throw new RuntimeException("权限不存在: " + permission.getId());
        }

        permissionMapper.update(permission);
        log.info("更新权限成功: {}", permission.getPermissionName());
        return permission;
    }

    /**
     * 删除权限
     */
    public void deletePermission(Long id) {
        permissionMapper.deleteById(id);
        log.info("删除权限成功: {}", id);
    }

    /**
     * 检查用户是否有指定权限
     */
    public boolean hasPermission(Long userId, String permissionCode) {
        List<Permission> permissions = permissionMapper.selectByUserId(userId);
        return permissions.stream()
                .anyMatch(p -> permissionCode.equals(p.getPermissionCode()));
    }
}
