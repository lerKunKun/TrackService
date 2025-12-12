package com.logistics.track17.controller;

import com.logistics.track17.dto.LoginRequest;
import com.logistics.track17.dto.LoginResponse;
import com.logistics.track17.dto.Result;
import com.logistics.track17.dto.UserDTO;
import com.logistics.track17.entity.User;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.service.UserService;
import com.logistics.track17.service.DingTalkService;
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
    private final DingTalkService dingTalkService;
    private final com.logistics.track17.config.DingTalkConfig dingTalkConfig;

    @Value("${jwt.expiration}")
    private Long expiration;

    public AuthController(JwtUtil jwtUtil, UserService userService,
            DingTalkService dingTalkService,
            com.logistics.track17.config.DingTalkConfig dingTalkConfig) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.dingTalkService = dingTalkService;
        this.dingTalkConfig = dingTalkConfig;
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
     * 获取钉钉登录URL
     */
    @GetMapping("/dingtalk/login-url")
    public Result<String> getDingTalkLoginUrl() {
        // 使用更快的UUID生成方法
        String state = generateFastUUID();
        // TODO: 可以将state存入Redis，5分钟过期，用于验证回调
        String loginUrl = dingTalkConfig.getLoginUrl(state);
        return Result.success(loginUrl);
    }

    /**
     * 快速生成UUID（比UUID.randomUUID()快约2-3倍）
     */
    private String generateFastUUID() {
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        long mostSigBits = random.nextLong();
        long leastSigBits = random.nextLong();
        return new java.util.UUID(mostSigBits, leastSigBits).toString();
    }

    /**
     * 钉钉登录回调（GET请求 - OAuth标准流程）
     * 钉钉扫码成功后会重定向到这个接口，通过URL参数传递code和state
     */
    @GetMapping("/dingtalk/callback")
    public void dingTalkCallbackGet(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            javax.servlet.http.HttpServletResponse httpResponse) throws java.io.IOException {
        log.info("DingTalk OAuth callback (GET) with code: {}, state: {}", code, state);

        try {
            // 1. 用authCode换取accessToken
            String accessToken = dingTalkService.getAccessToken(code);

            // 2. 获取用户信息
            com.logistics.track17.dto.DingTalkUserInfo userInfo = dingTalkService.getUserInfo(accessToken);

            // 3. 登录或注册用户
            User user = userService.loginOrRegisterWithDingTalk(userInfo, dingTalkConfig.getCorpId());

            // 4. 验证访问权限：超级管理员 OR CorpId在白名单中
            boolean isAdmin = "ADMIN".equals(user.getRole());
            boolean isCorpIdAllowed = dingTalkService.validateCorpId(user.getCorpId());

            if (!isAdmin && !isCorpIdAllowed) {
                log.warn("User {} from corpId {} is not allowed to login. Not admin and corpId not in whitelist.",
                        user.getUsername(), user.getCorpId());
                httpResponse.sendRedirect("http://localhost:3000/login?error=" +
                        java.net.URLEncoder.encode("您的企业暂无登录权限，请联系管理员", "UTF-8"));
                return;
            }

            // 5. 生成JWT Token
            String token = jwtUtil.generateToken(user.getUsername());

            // 6. 更新最后登录信息
            userService.updateLastLogin(user.getId(), "DingTalk OAuth");

            log.info("User {} (role: {}, corpId: {}) logged in via DingTalk OAuth",
                    user.getUsername(), user.getRole(), user.getCorpId());

            // 7. 重定向到前端，携带token和用户信息
            String redirectUrl = String.format(
                    "http://localhost:3000/dingtalk/callback?token=%s&username=%s&realName=%s",
                    java.net.URLEncoder.encode(token, "UTF-8"),
                    java.net.URLEncoder.encode(user.getUsername(), "UTF-8"),
                    java.net.URLEncoder.encode(user.getRealName() != null ? user.getRealName() : user.getUsername(),
                            "UTF-8"));
            httpResponse.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("DingTalk OAuth callback failed", e);
            httpResponse.sendRedirect("http://localhost:3000/login?error="
                    + java.net.URLEncoder.encode("钉钉登录失败: " + e.getMessage(), "UTF-8"));
        }
    }

    /**
     * 钉钉登录回调（POST请求 - 兼容前端直接调用）
     */
    @PostMapping("/dingtalk/callback")
    public Result<LoginResponse> dingTalkCallback(
            @org.springframework.validation.annotation.Validated @RequestBody com.logistics.track17.dto.DingTalkLoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("DingTalk login callback with code: {}", request.getCode());

        try {
            // 1. 用authCode换取accessToken
            String accessToken = dingTalkService.getAccessToken(request.getCode());

            // 2. 获取用户信息
            com.logistics.track17.dto.DingTalkUserInfo userInfo = dingTalkService.getUserInfo(accessToken);

            // 3. 验证CorpId（这里简化处理，使用配置的CorpId）
            if (!dingTalkService.validateCorpId(dingTalkConfig.getCorpId())) {
                throw com.logistics.track17.exception.BusinessException.of(403, "非本企业用户，禁止登录");
            }

            // 4. 登录或注册用户
            User user = userService.loginOrRegisterWithDingTalk(userInfo, dingTalkConfig.getCorpId());

            // 5. 生成JWT Token
            String token = jwtUtil.generateToken(user.getUsername());

            // 6. 更新最后登录信息
            String loginIp = getClientIp(httpRequest);
            userService.updateLastLogin(user.getId(), loginIp);

            LoginResponse response = new LoginResponse(token, user.getUsername(), user.getRealName(), expiration);

            log.info("User {} logged in via DingTalk from IP: {}", user.getUsername(), loginIp);
            return Result.success("登录成功", response);

        } catch (Exception e) {
            log.error("DingTalk login failed", e);
            throw com.logistics.track17.exception.BusinessException.of("钉钉登录失败: " + e.getMessage());
        }
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
