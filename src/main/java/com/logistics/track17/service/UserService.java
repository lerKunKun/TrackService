package com.logistics.track17.service;

import com.logistics.track17.dto.*;
import com.logistics.track17.entity.User;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Slf4j
@Service
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 根据用户名查询用户（用于登录验证）
     */
    public User getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    /**
     * 获取用于系统回退的用户ID（优先 admin，其次任意存在的用户）
     */
    public Long getFallbackUserId() {
        User adminUser = getUserByUsername("admin");
        if (adminUser != null) {
            return adminUser.getId();
        }

        List<User> users = userMapper.selectAll();
        if (users == null || users.isEmpty()) {
            return null;
        }

        return users.get(0).getId();
    }

    /**
     * 根据ID查询用户
     */
    public UserDTO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }
        return convertToDTO(user);
    }

    /**
     * 验证密码
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 创建用户
     */
    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        // 检查用户名是否已存在
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw BusinessException.of(400, "用户名已存在");
        }

        // 检查邮箱是否已存在
        if (request.getEmail() != null && userMapper.selectByEmail(request.getEmail()) != null) {
            throw BusinessException.of(400, "邮箱已被使用");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // BCrypt加密
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRealName(request.getRealName());
        user.setRole(request.getRole() != null ? request.getRole() : "USER");
        user.setStatus(1); // 默认启用

        int result = userMapper.insert(user);
        if (result > 0) {
            log.info("Created user: {}", user.getUsername());
            return convertToDTO(user);
        } else {
            throw BusinessException.of(500, "创建用户失败");
        }
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }

        // 检查邮箱是否已被其他用户使用
        if (request.getEmail() != null) {
            User existingUser = userMapper.selectByEmail(request.getEmail());
            if (existingUser != null && !existingUser.getId().equals(id)) {
                throw BusinessException.of(400, "邮箱已被使用");
            }
        }

        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setEmail(request.getEmail());
        updateUser.setPhone(request.getPhone());
        updateUser.setRealName(request.getRealName());
        updateUser.setAvatar(request.getAvatar());

        int result = userMapper.update(updateUser);
        if (result > 0) {
            log.info("Updated user: {}", id);
            return getUserById(id);
        } else {
            throw BusinessException.of(500, "更新用户失败");
        }
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw BusinessException.of(400, "旧密码错误");
        }

        // 更新密码
        String newEncodedPassword = passwordEncoder.encode(request.getNewPassword());
        int result = userMapper.updatePassword(id, newEncodedPassword);
        if (result > 0) {
            log.info("Password changed for user: {}", id);
        } else {
            throw BusinessException.of(500, "修改密码失败");
        }
    }

    /**
     * 更新最后登录信息
     */
    @Transactional
    public void updateLastLogin(Long id, String loginIp) {
        int result = userMapper.updateLastLogin(id, LocalDateTime.now(), loginIp);
        if (result > 0) {
            log.debug("Updated last login for user: {}", id);
        }
    }

    /**
     * 更新用户状态
     */
    @Transactional
    public void updateUserStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }

        int result = userMapper.updateStatus(id, status);
        if (result > 0) {
            log.info("Updated status for user {}: {}", id, status);
        } else {
            throw BusinessException.of(500, "更新状态失败");
        }
    }

    /**
     * 钉钉登录或注册用户
     */
    @Transactional
    public User loginOrRegisterWithDingTalk(DingTalkUserInfo dingTalkUserInfo, String corpId) {
        log.info("DingTalk login/register for unionId: {}", dingTalkUserInfo.getUnionId());

        // 1. 根据unionId查找用户
        User user = userMapper.selectByDingUnionId(dingTalkUserInfo.getUnionId());

        if (user != null) {
            // 用户已存在，更新登录信息
            log.info("Existing DingTalk user found: {}", user.getUsername());
            userMapper.updateLastLogin(user.getId(), LocalDateTime.now(), null);
            return user;
        }

        // 2. 用户不存在，创建新用户
        user = new User();
        user.setDingUnionId(dingTalkUserInfo.getUnionId());
        user.setCorpId(corpId);

        // 生成唯一的用户名
        String username = generateUniqueUsername(dingTalkUserInfo);

        // 检查username是否已被占用
        User existingUserWithSameName = userMapper.selectByUsername(username);
        if (existingUserWithSameName != null) {
            // username已存在，检查是否是同一个钉钉用户
            if (dingTalkUserInfo.getUnionId().equals(existingUserWithSameName.getDingUnionId())) {
                // 并发情况：另一个请求已经创建了这个用户
                log.info("Concurrent registration detected, returning existing user: {}",
                        existingUserWithSameName.getUsername());
                return existingUserWithSameName;
            } else {
                // 不同的钉钉用户，username冲突，添加手机号后缀
                String phoneSuffix = "";
                if (dingTalkUserInfo.getMobile() != null && dingTalkUserInfo.getMobile().length() >= 4) {
                    phoneSuffix = dingTalkUserInfo.getMobile().substring(dingTalkUserInfo.getMobile().length() - 4);
                } else {
                    // 如果没有手机号，使用随机数
                    phoneSuffix = String.valueOf((int) (Math.random() * 10000));
                }
                username = dingTalkUserInfo.getNick() + "_" + phoneSuffix;
                log.warn("Username conflict, generated new username with suffix: {}", username);
            }
        }

        user.setUsername(username);
        user.setRealName(dingTalkUserInfo.getNick());
        user.setEmail(dingTalkUserInfo.getEmail());
        user.setPhone(dingTalkUserInfo.getMobile());
        user.setAvatar(dingTalkUserInfo.getAvatarUrl());
        user.setLoginSource("DINGTALK");
        user.setRole("USER"); // 默认普通用户
        user.setStatus(1); // 默认启用
        // 随机密码,钉钉用户不需要密码登录
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));

        try {
            userMapper.insert(user);
            log.info("Created new user from DingTalk: {}", user.getUsername());
            return user;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 极端并发情况：在检查和插入之间，另一个请求创建了相同username
            log.warn("Duplicate key after check, attempting final unionId lookup");
            User finalCheck = userMapper.selectByDingUnionId(dingTalkUserInfo.getUnionId());
            if (finalCheck != null) {
                log.info("Found user by unionId on final check: {}", finalCheck.getUsername());
                return finalCheck;
            }
            // 如果还是找不到，抛出原始异常
            throw e;
        }
    }

    /**
     * 删除用户（软删除）
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }

        // 不允许删除管理员账号
        if ("ADMIN".equals(user.getRole())) {
            throw BusinessException.of(400, "不允许删除管理员账号");
        }

        // 软删除：设置 deleted_at 时间戳
        int result = userMapper.deleteById(id);
        if (result > 0) {
            log.info("Soft deleted user: {}", id);
        } else {
            throw BusinessException.of(500, "删除用户失败");
        }
    }

    /**
     * 获取所有用户列表
     */
    public List<UserDTO> getAllUsers() {
        List<User> users = userMapper.selectAll();
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询用户列表
     */
    public List<UserDTO> getUsersByPage(int page, int size) {
        int offset = (page - 1) * size;
        List<User> users = userMapper.selectByPage(offset, size);
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 统计用户总数
     */
    public Long getUserCount() {
        return userMapper.count();
    }

    /**
     * 生成唯一的用户名（钉钉用户）
     * 优先使用昵称，如果昵称已存在则使用 昵称_手机号后4位
     */
    private String generateUniqueUsername(DingTalkUserInfo dingTalkUserInfo) {
        String baseUsername = dingTalkUserInfo.getNick();

        // 检查昵称是否已存在
        User existingUser = userMapper.selectByUsername(baseUsername);
        if (existingUser == null) {
            return baseUsername;
        }

        // 昵称已存在，使用手机号后4位或随机数作为后缀
        String suffix = "";
        if (dingTalkUserInfo.getMobile() != null && dingTalkUserInfo.getMobile().length() >= 4) {
            suffix = dingTalkUserInfo.getMobile().substring(dingTalkUserInfo.getMobile().length() - 4);
        } else {
            // 最后的fallback：使用随机数
            suffix = String.valueOf((int) (Math.random() * 10000));
        }

        return baseUsername + "_" + suffix;
    }

    /**
     * 将User实体转换为UserDTO（去除密码）
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        // 密码字段不会被复制，因为UserDTO中没有password字段
        return dto;
    }

    /**
     * 获取用户的角色列表
     */
    public List<com.logistics.track17.entity.Role> getUserRoles(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }
        // 通过 RoleMapper 查询用户角色
        return userMapper.selectRolesByUserId(userId);
    }

    /**
     * 更新用户的角色列表
     */
    @Transactional
    public void updateUserRoles(Long userId, List<Long> roleIds) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }
        // 删除用户现有角色
        userMapper.deleteUserRoles(userId);
        // 添加新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                userMapper.insertUserRole(userId, roleId);
            }
        }
        log.info("Updated roles for user {}: {}", userId, roleIds);
    }

    /**
     * 获取用户列表（带角色信息）- 优化版，避免 N+1 查询
     */
    public java.util.Map<String, Object> getUsersWithRoles(int page, int size) {
        int offset = (page - 1) * size;
        List<User> users = userMapper.selectByPage(offset, size);
        Long total = userMapper.count();

        if (users.isEmpty()) {
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("list", java.util.Collections.emptyList());
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            return result;
        }

        // 批量获取所有用户的角色（单次查询）
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        List<java.util.Map<String, Object>> userRoleMappings = userMapper.selectRolesByUserIds(userIds);

        // 构建用户ID到角色列表的映射
        java.util.Map<Long, List<com.logistics.track17.entity.Role>> userRolesMap = new java.util.HashMap<>();
        for (java.util.Map<String, Object> mapping : userRoleMappings) {
            Long userId = ((Number) mapping.get("user_id")).longValue();
            com.logistics.track17.entity.Role role = new com.logistics.track17.entity.Role();
            role.setId(((Number) mapping.get("role_id")).longValue());
            role.setRoleName((String) mapping.get("role_name"));
            role.setRoleCode((String) mapping.get("role_code"));
            role.setDescription((String) mapping.get("description"));
            // 处理 status 字段（可能是 Boolean 或 Number，取决于 JDBC 驱动）
            Object statusObj = mapping.get("status");
            if (statusObj instanceof Boolean) {
                role.setStatus(((Boolean) statusObj) ? 1 : 0);
            } else if (statusObj instanceof Number) {
                role.setStatus(((Number) statusObj).intValue());
            } else {
                role.setStatus(1);
            }

            userRolesMap.computeIfAbsent(userId, k -> new java.util.ArrayList<>()).add(role);
        }

        // 构建返回结果
        List<java.util.Map<String, Object>> usersWithRoles = users.stream()
                .map(user -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    UserDTO dto = convertToDTO(user);
                    map.put("id", dto.getId());
                    map.put("username", dto.getUsername());
                    map.put("realName", dto.getRealName());
                    map.put("email", dto.getEmail());
                    map.put("phone", dto.getPhone());
                    map.put("avatar", dto.getAvatar());
                    map.put("status", dto.getStatus());
                    // 从映射中获取角色，避免 N+1 查询
                    map.put("roles", userRolesMap.getOrDefault(user.getId(), java.util.Collections.emptyList()));
                    return map;
                })
                .collect(Collectors.toList());

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("list", usersWithRoles);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
}
