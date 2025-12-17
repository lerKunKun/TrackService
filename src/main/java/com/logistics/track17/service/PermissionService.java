package com.logistics.track17.service;

import com.logistics.track17.entity.Permission;
import com.logistics.track17.mapper.PermissionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

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

    /**
     * 检查用户是否拥有任一权限（OR逻辑）
     * 
     * @param userId          用户ID
     * @param permissionCodes 权限码数组
     * @return 只要拥有其中一个权限即返回true
     */
    public boolean hasAnyPermission(Long userId, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }

        List<Permission> permissions = permissionMapper.selectByUserId(userId);
        Set<String> userPermissions = permissions.stream()
                .map(Permission::getPermissionCode)
                .collect(java.util.stream.Collectors.toSet());

        return Arrays.stream(permissionCodes)
                .anyMatch(userPermissions::contains);
    }

    /**
     * 检查用户是否拥有所有权限（AND逻辑）
     * 
     * @param userId          用户ID
     * @param permissionCodes 权限码数组
     * @return 拥有所有指定权限才返回true
     */
    public boolean hasAllPermissions(Long userId, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }

        List<Permission> permissions = permissionMapper.selectByUserId(userId);
        Set<String> userPermissions = permissions.stream()
                .map(Permission::getPermissionCode)
                .collect(java.util.stream.Collectors.toSet());

        return Arrays.stream(permissionCodes)
                .allMatch(userPermissions::contains);
    }

    /**
     * 获取用户权限码集合（用于缓存）
     * 
     * @param userId 用户ID
     * @return 用户所有权限码的Set集合
     */
    public Set<String> getUserPermissionCodes(Long userId) {
        List<Permission> permissions = permissionMapper.selectByUserId(userId);
        return permissions.stream()
                .map(Permission::getPermissionCode)
                .collect(java.util.stream.Collectors.toSet());
    }
}
