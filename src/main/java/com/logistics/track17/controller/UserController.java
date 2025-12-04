package com.logistics.track17.controller;

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
    public Result<List<UserDTO>> getAllUsers() {
        log.info("Get all users");
        List<UserDTO> users = userService.getAllUsers();
        return Result.success(users);
    }

    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        log.info("Get user by id: {}", id);
        UserDTO user = userService.getUserById(id);
        return Result.success(user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<UserDTO> createUser(@Validated @RequestBody CreateUserRequest request) {
        log.info("Create user: {}", request.getUsername());
        UserDTO user = userService.createUser(request);
        return Result.success("用户创建成功", user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
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
    public Result<Object> deleteUser(@PathVariable Long id) {
        log.info("Delete user: {}", id);
        userService.deleteUser(id);
        return Result.success("用户删除成功", null);
    }
}
