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
        user.setUsername(dingTalkUserInfo.getNick() + "_" + System.currentTimeMillis());
        user.setRealName(dingTalkUserInfo.getNick());
        user.setEmail(dingTalkUserInfo.getEmail());
        user.setPhone(dingTalkUserInfo.getMobile());
        user.setAvatar(dingTalkUserInfo.getAvatarUrl());
        user.setLoginSource("DINGTALK");
        user.setRole("USER"); // 默认普通用户
        user.setStatus(1); // 默认启用
        // 随机密码，钉钉用户不需要密码登录
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));

        userMapper.insert(user);
        log.info("Created new user from DingTalk: {}", user.getUsername());

        return user;
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
     * 将User实体转换为UserDTO（去除密码）
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        // 密码字段不会被复制，因为UserDTO中没有password字段
        return dto;
    }
}
