package com.logistics.track17.util;

import com.logistics.track17.annotation.DataPermission;
import com.logistics.track17.entity.Role;
import com.logistics.track17.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限辅助工具类
 * 提供数据权限过滤的便捷方法
 */
@Component
public class DataPermissionHelper {

    @Autowired
    private RoleService roleService;

    /**
     * 检查当前用户是否有全部数据权限
     * 
     * @return true 如果用户是管理员或拥有全部数据权限
     */
    public boolean hasAllDataPermission() {
        Long userId = UserContextHolder.getCurrentUserId();
        if (userId == null) {
            return false;
        }

        // 检查是否是管理员
        List<Role> roles = roleService.getRolesByUserId(userId);
        Set<String> roleCodes = roles.stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toSet());

        return roleCodes.contains("SUPER_ADMIN") || roleCodes.contains("ADMIN");
    }

    /**
     * 获取当前用户可见的用户ID列表（根据数据权限范围）
     * 
     * @param scope 数据权限范围
     * @return 用户ID列表
     */
    public List<Long> getAccessibleUserIds(DataPermission.DataScope scope) {
        Long currentUserId = UserContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("未登录");
        }

        // 如果有全部数据权限，返回 null 表示不限制
        if (hasAllDataPermission()) {
            return null;
        }

        switch (scope) {
            case ALL:
                // 需要管理员权限
                if (hasAllDataPermission()) {
                    return null; // null 表示不限制
                }
                throw new RuntimeException("无权访问全部数据");

            case SELF:
                // 仅本人
                return List.of(currentUserId);

            case DEPT:
            case DEPT_AND_CHILD:
                // TODO: 实现部门数据权限（需要部门表支持）
                // 当前简化实现：仅返回本人
                return List.of(currentUserId);

            case CUSTOM:
            default:
                return List.of(currentUserId);
        }
    }

    /**
     * 检查当前用户是否可以访问指定用户ID的数据
     * 
     * @param targetUserId 目标用户ID
     * @param scope        数据权限范围
     * @return true 如果可以访问
     */
    public boolean canAccessUserData(Long targetUserId, DataPermission.DataScope scope) {
        if (targetUserId == null) {
            return false;
        }

        Long currentUserId = UserContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        // 管理员可以访问所有数据
        if (hasAllDataPermission()) {
            return true;
        }

        // 根据数据范围检查
        List<Long> accessibleUserIds = getAccessibleUserIds(scope);
        return accessibleUserIds == null || accessibleUserIds.contains(targetUserId);
    }
}
