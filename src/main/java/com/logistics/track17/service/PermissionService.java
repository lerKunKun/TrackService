package com.logistics.track17.service;

import com.logistics.track17.entity.Permission;
import com.logistics.track17.mapper.PermissionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
     * 使用缓存提升性能
     */
    @Cacheable(value = "user:permissions", key = "#userId")
    public List<Permission> getPermissionsByUserId(Long userId) {
        return permissionMapper.selectByUserId(userId);
    }

    /**
     * 创建权限
     * 创建后清除所有用户的权限缓存（因为可能通过角色间接影响）
     */
    @CacheEvict(value = "user:permissions", allEntries = true)
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
     * 更新后清除所有用户的权限缓存
     */
    @CacheEvict(value = "user:permissions", allEntries = true)
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
     * 删除后清除所有用户的权限缓存
     */
    @CacheEvict(value = "user:permissions", allEntries = true)
    public void deletePermission(Long id) {
        permissionMapper.deleteById(id);
        log.info("删除权限成功: {}", id);
    }

    /**
     * 获取用户权限码集合（用于缓存）
     * 返回用户所有权限码的 Set 集合，便于权限检查
     * 缓存 30 分钟，key 格式：track17:user:permissions:codes:{userId}
     * 
     * @param userId 用户ID
     * @return 用户所有权限码的Set集合
     */
    @Cacheable(value = "user:permissions:codes", key = "#userId")
    public Set<String> getUserPermissionCodes(Long userId) {
        List<Permission> permissions = permissionMapper.selectByUserId(userId);
        return permissions.stream()
                .map(Permission::getPermissionCode)
                .collect(java.util.stream.Collectors.toSet());
    }
}
