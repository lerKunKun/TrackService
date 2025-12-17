package com.logistics.track17.controller;

import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.Permission;
import com.logistics.track17.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理Controller
 */
@RestController
@RequestMapping("/permissions")
@Slf4j
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    /**
     * 获取所有权限
     */
    @GetMapping
    @RequireAuth(permissions = "system:permission:view")
    public ResponseEntity<Result<List<Permission>>> getAllPermissions() {
        try {
            List<Permission> permissions = permissionService.getAllPermissions();
            return ResponseEntity.ok(Result.success("获取权限列表成功", permissions));
        } catch (Exception e) {
            log.error("获取权限列表失败", e);
            return ResponseEntity.ok(Result.error(500, "获取权限列表失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取权限
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = "system:permission:view")
    public ResponseEntity<Result<Permission>> getPermissionById(@PathVariable Long id) {
        try {
            Permission permission = permissionService.getPermissionById(id);
            if (permission == null) {
                return ResponseEntity.ok(Result.error(404, "权限不存在"));
            }
            return ResponseEntity.ok(Result.success("获取权限成功", permission));
        } catch (Exception e) {
            log.error("获取权限失败", e);
            return ResponseEntity.ok(Result.error(500, "获取权限失败: " + e.getMessage()));
        }
    }

    /**
     * 创建权限
     */
    @PostMapping
    @RequireAuth(permissions = "system:permission:create")
    public ResponseEntity<Result<Permission>> createPermission(@RequestBody Permission permission) {
        try {
            Permission created = permissionService.createPermission(permission);
            return ResponseEntity.ok(Result.success("创建权限成功", created));
        } catch (Exception e) {
            log.error("创建权限失败", e);
            return ResponseEntity.ok(Result.error(500, "创建权限失败: " + e.getMessage()));
        }
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @RequireAuth(permissions = "system:permission:update")
    public ResponseEntity<Result<Permission>> updatePermission(@PathVariable Long id,
            @RequestBody Permission permission) {
        try {
            permission.setId(id);
            Permission updated = permissionService.updatePermission(permission);
            return ResponseEntity.ok(Result.success("更新权限成功", updated));
        } catch (Exception e) {
            log.error("更新权限失败", e);
            return ResponseEntity.ok(Result.error(500, "更新权限失败: " + e.getMessage()));
        }
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @RequireAuth(permissions = "system:permission:delete")
    public ResponseEntity<Result<Void>> deletePermission(@PathVariable Long id) {
        try {
            permissionService.deletePermission(id);
            return ResponseEntity.ok(Result.success("删除权限成功", null));
        } catch (Exception e) {
            log.error("删除权限失败", e);
            return ResponseEntity.ok(Result.error(500, "删除权限失败: " + e.getMessage()));
        }
    }
}
