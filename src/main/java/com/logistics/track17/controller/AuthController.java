package com.logistics.track17.controller;

import com.logistics.track17.dto.LoginRequest;
import com.logistics.track17.dto.LoginResponse;
import com.logistics.track17.dto.Result;
import com.logistics.track17.dto.UserDTO;
import com.logistics.track17.entity.User;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.service.UserService;
import com.logistics.track17.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Value("${jwt.expiration}")
    private Long expiration;

    public AuthController(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    /**
     * 用户登录
     *
     * 使用数据库验证用户名密码
     * 密码使用BCrypt加密存储
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for user: {}", request.getUsername());

        // 从数据库查询用户
        User user = userService.getUserByUsername(request.getUsername());
        if (user == null) {
            throw BusinessException.of(401, "用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw BusinessException.of(403, "账号已被禁用，请联系管理员");
        }

        // 验证密码（BCrypt）
        if (!userService.verifyPassword(request.getPassword(), user.getPassword())) {
            throw BusinessException.of(401, "用户名或密码错误");
        }

        // 生成Token
        String token = jwtUtil.generateToken(user.getUsername());

        // 更新最后登录信息
        String loginIp = getClientIp(httpRequest);
        userService.updateLastLogin(user.getId(), loginIp);

        LoginResponse response = new LoginResponse(token, user.getUsername(), user.getRealName(), expiration);

        log.info("User {} logged in successfully from IP: {}", user.getUsername(), loginIp);
        return Result.success("登录成功", response);
    }

    /**
     * 验证Token
     */
    @GetMapping("/validate")
    public Result<Boolean> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.success(false);
        }

        String token = authHeader.substring(7);
        boolean valid = jwtUtil.validateToken(token);

        return Result.success(valid);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Result<UserDTO> getCurrentUser(@RequestAttribute("username") String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }

        UserDTO userDTO = userService.getUserById(user.getId());
        return Result.success(userDTO);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
