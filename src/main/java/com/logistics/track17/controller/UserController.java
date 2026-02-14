package com.logistics.track17.controller;

import com.logistics.track17.annotation.AuditLog;
import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.dto.*;
import com.logistics.track17.entity.User;
import com.logistics.track17.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取用户列表（分页）
     */
    @GetMapping
    @RequireAuth(permissions = "system:user:view")
    public Result<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get users list, page: {}, size: {}", page, size);

        List<UserDTO> users = userService.getUsersByPage(page, size);
        Long total = userService.getUserCount();

        Map<String, Object> data = new HashMap<>();
        data.put("list", users);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);

        return Result.success(data);
    }

    /**
     * 获取所有用户列表（不分页）
     */
    @GetMapping("/all")
    @RequireAuth(permissions = "system:user:view")
    public Result<List<UserDTO>> getAllUsers() {
        log.info("Get all users");
        List<UserDTO> users = userService.getAllUsers();
        return Result.success(users);
    }

    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = "system:user:view")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        log.info("Get user by id: {}", id);
        UserDTO user = userService.getUserById(id);
        return Result.success(user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    @RequireAuth(permissions = "system:user:create")
    @AuditLog(operation = "创建用户", module = "用户管理")
    public Result<UserDTO> createUser(@Validated @RequestBody CreateUserRequest request) {
        log.info("Create user: {}", request.getUsername());
        UserDTO user = userService.createUser(request);
        return Result.success("用户创建成功", user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @RequireAuth(permissions = "system:user:update")
    @AuditLog(operation = "更新用户信息", module = "用户管理")
    public Result<UserDTO> updateUser(
            @PathVariable Long id,
            @Validated @RequestBody UpdateUserRequest request) {
        log.info("Update user: {}", id);
        UserDTO user = userService.updateUser(id, request);
        return Result.success("用户更新成功", user);
    }

    /**
     * 修改密码
     */
    @PostMapping("/{id}/password")
    @RequireAuth(permissions = "system:user:update")
    @AuditLog(operation = "修改用户密码", module = "用户管理", logParams = false)
    public Result<Object> changePassword(
            @PathVariable Long id,
            @Validated @RequestBody ChangePasswordRequest request) {
        log.info("Change password for user: {}", id);
        userService.changePassword(id, request);
        return Result.success("密码修改成功", null);
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/{id}/status")
    @RequireAuth(permissions = "system:user:update")
    public Result<Object> updateUserStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        log.info("Update user {} status to: {}", id, status);
        userService.updateUserStatus(id, status);
        return Result.success("状态更新成功", null);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @RequireAuth(permissions = "system:user:delete")
    @AuditLog(operation = "删除用户", module = "用户管理")
    public Result<Object> deleteUser(@PathVariable Long id) {
        log.info("Delete user: {}", id);
        userService.deleteUser(id);
        return Result.success("用户删除成功", null);
    }

    /**
     * 获取用户的角色列表
     */
    @GetMapping("/{id}/roles")
    @RequireAuth(permissions = "system:user:view")
    public Result<List<com.logistics.track17.entity.Role>> getUserRoles(@PathVariable Long id) {
        log.info("Get roles for user: {}", id);
        List<com.logistics.track17.entity.Role> roles = userService.getUserRoles(id);
        return Result.success(roles);
    }

    /**
     * 更新用户的角色列表
     */
    @PutMapping("/{id}/roles")
    @RequireAuth(permissions = "system:role:assign")
    @AuditLog(operation = "更新用户角色", module = "用户管理")
    public Result<Object> updateUserRoles(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> request) {
        log.info("Update roles for user: {}, roles: {}", id, request.get("roleIds"));
        List<Long> roleIds = request.get("roleIds");
        userService.updateUserRoles(id, roleIds);
        return Result.success("角色更新成功", null);
    }

    /**
     * 获取用户列表（带角色信息）
     */
    @GetMapping("/with-roles")
    @RequireAuth(permissions = "system:user:view")
    public Result<Map<String, Object>> getUsersWithRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        log.info("Get users with roles, page: {}, size: {}", page, size);
        Map<String, Object> data = userService.getUsersWithRoles(page, size);
        return Result.success(data);
    }

    /**
     * 获取所有用户列表（带角色信息）- 不分页
     */
    @GetMapping("/all-with-roles")
    @RequireAuth(permissions = "system:user:view")
    public Result<Map<String, Object>> getAllUsersWithRoles() {
        log.info("Get all users with roles");
        Map<String, Object> data = userService.getAllUsersWithRoles();
        return Result.success(data);
    }
}
