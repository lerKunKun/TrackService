package com.logistics.track17.controller;

import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.Menu;
import com.logistics.track17.entity.Permission;
import com.logistics.track17.entity.Role;
import com.logistics.track17.service.MenuService;
import com.logistics.track17.service.PermissionService;
import com.logistics.track17.service.RoleService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理Controller
 */
@RestController
@RequestMapping("/roles")
@Slf4j
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private PermissionService permissionService;

    /**
     * 获取所有角色
     */
    @GetMapping
    @RequireAuth(permissions = "system:role:view")
    public ResponseEntity<Result<List<Role>>> getAllRoles() {
        try {
            List<Role> roles = roleService.getAllRoles();
            return ResponseEntity.ok(Result.success("获取角色列表成功", roles));
        } catch (Exception e) {
            log.error("获取角色列表失败", e);
            return ResponseEntity.ok(Result.error(500, "获取角色列表失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取角色
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = "system:role:view")
    public ResponseEntity<Result<Role>> getRoleById(@PathVariable Long id) {
        try {
            Role role = roleService.getRoleById(id);
            if (role == null) {
                return ResponseEntity.ok(Result.error(404, "角色不存在"));
            }
            return ResponseEntity.ok(Result.success("获取角色成功", role));
        } catch (Exception e) {
            log.error("获取角色失败", e);
            return ResponseEntity.ok(Result.error(500, "获取角色失败: " + e.getMessage()));
        }
    }

    /**
     * 创建角色
     */
    @PostMapping
    @RequireAuth(permissions = "system:role:create")
    public ResponseEntity<Result<Role>> createRole(@RequestBody Role role) {
        try {
            Role created = roleService.createRole(role);
            return ResponseEntity.ok(Result.success("创建角色成功", created));
        } catch (Exception e) {
            log.error("创建角色失败", e);
            return ResponseEntity.ok(Result.error(500, "创建角色失败: " + e.getMessage()));
        }
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @RequireAuth(permissions = "system:role:update")
    public ResponseEntity<Result<Role>> updateRole(@PathVariable Long id, @RequestBody Role role) {
        try {
            role.setId(id);
            Role updated = roleService.updateRole(role);
            return ResponseEntity.ok(Result.success("更新角色成功", updated));
        } catch (Exception e) {
            log.error("更新角色失败", e);
            return ResponseEntity.ok(Result.error(500, "更新角色失败: " + e.getMessage()));
        }
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @RequireAuth(permissions = "system:role:delete")
    public ResponseEntity<Result<Void>> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.ok(Result.success("删除角色成功", null));
        } catch (Exception e) {
            log.error("删除角色失败", e);
            return ResponseEntity.ok(Result.error(500, "删除角色失败: " + e.getMessage()));
        }
    }

    /**
     * 获取角色的菜单
     */
    @GetMapping("/{id}/menus")
    @RequireAuth(permissions = "system:role:view")
    public ResponseEntity<Result<List<Menu>>> getRoleMenus(@PathVariable Long id) {
        try {
            List<Menu> menus = menuService.getMenusByRoleId(id);
            return ResponseEntity.ok(Result.success("获取角色菜单成功", menus));
        } catch (Exception e) {
            log.error("获取角色菜单失败", e);
            return ResponseEntity.ok(Result.error(500, "获取角色菜单失败: " + e.getMessage()));
        }
    }

    /**
     * 为角色分配菜单
     */
    @PostMapping("/{id}/menus")
    @RequireAuth(permissions = "system:role:assign-menu")
    public ResponseEntity<Result<Void>> assignMenusToRole(@PathVariable Long id,
            @RequestBody AssignMenusRequest request) {
        try {
            roleService.assignMenusToRole(id, request.getMenuIds());
            return ResponseEntity.ok(Result.success("分配菜单成功", null));
        } catch (Exception e) {
            log.error("分配菜单失败", e);
            return ResponseEntity.ok(Result.error(500, "分配菜单失败: " + e.getMessage()));
        }
    }

    /**
     * 获取角色的权限
     */
    @GetMapping("/{id}/permissions")
    @RequireAuth(permissions = "system:role:view")
    public ResponseEntity<Result<List<Permission>>> getRolePermissions(@PathVariable Long id) {
        try {
            List<Permission> permissions = permissionService.getPermissionsByRoleId(id);
            return ResponseEntity.ok(Result.success("获取角色权限成功", permissions));
        } catch (Exception e) {
            log.error("获取角色权限失败", e);
            return ResponseEntity.ok(Result.error(500, "获取角色权限失败: " + e.getMessage()));
        }
    }

    /**
     * 为角色分配权限
     */
    @PostMapping("/{id}/permissions")
    @RequireAuth(permissions = "system:role:assign-permission")
    public ResponseEntity<Result<Void>> assignPermissionsToRole(@PathVariable Long id,
            @RequestBody AssignPermissionsRequest request) {
        try {
            roleService.assignPermissionsToRole(id, request.getPermissionIds());
            return ResponseEntity.ok(Result.success("分配权限成功", null));
        } catch (Exception e) {
            log.error("分配权限失败", e);
            return ResponseEntity.ok(Result.error(500, "分配权限失败: " + e.getMessage()));
        }
    }

    /**
     * 更新角色权限（PUT方式）
     */
    @PutMapping("/{id}/permissions")
    @RequireAuth(permissions = "system:role:assign-permission")
    public ResponseEntity<Result<Void>> updateRolePermissions(@PathVariable Long id,
            @RequestBody AssignPermissionsRequest request) {
        try {
            roleService.assignPermissionsToRole(id, request.getPermissionIds());
            return ResponseEntity.ok(Result.success("更新权限成功", null));
        } catch (Exception e) {
            log.error("更新权限失败", e);
            return ResponseEntity.ok(Result.error(500, "更新权限失败: " + e.getMessage()));
        }
    }

    /**
     * 菜单分配请求
     */
    @Data
    public static class AssignMenusRequest {
        private List<Long> menuIds;
    }

    /**
     * 权限分配请求
     */
    @Data
    public static class AssignPermissionsRequest {
        private List<Long> permissionIds;
    }
}
