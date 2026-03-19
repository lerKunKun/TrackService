package com.logistics.track17.controller;

import com.logistics.track17.dto.LoginRequest;
import com.logistics.track17.dto.LoginResponse;
import com.logistics.track17.dto.Result;
import com.logistics.track17.dto.UserDTO;
import com.logistics.track17.entity.User;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.service.UserService;
import com.logistics.track17.service.DingTalkService;
import com.logistics.track17.service.LoginLogService;
import com.logistics.track17.service.OnlineSessionService;
import com.logistics.track17.service.PermissionService;
import com.logistics.track17.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

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
    private final PermissionService permissionService;
    private final LoginLogService loginLogService;
    private final OnlineSessionService onlineSessionService;
    private final com.logistics.track17.config.DingTalkConfig dingTalkConfig;
    private final com.logistics.track17.service.RoleService roleService;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${dingtalk.frontend-redirect:http://localhost:3000}")
    private String dingtalkFrontendRedirect;

    private static final String DINGTALK_STATE_PREFIX = "dingtalk:state:";
    private static final long DINGTALK_STATE_EXPIRE_SECONDS = 300;

    private final RedisTemplate<String, Object> redisTemplate;

    public AuthController(JwtUtil jwtUtil, UserService userService,
            DingTalkService dingTalkService,
            PermissionService permissionService,
            LoginLogService loginLogService,
            OnlineSessionService onlineSessionService,
            com.logistics.track17.config.DingTalkConfig dingTalkConfig,
            com.logistics.track17.service.RoleService roleService,
            RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.dingTalkService = dingTalkService;
        this.permissionService = permissionService;
        this.loginLogService = loginLogService;
        this.onlineSessionService = onlineSessionService;
        this.dingTalkConfig = dingTalkConfig;
        this.roleService = roleService;
        this.redisTemplate = redisTemplate;
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
        String loginIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // 从数据库查询用户
        User user = userService.getUserByUsername(request.getUsername());
        if (user == null) {
            loginLogService.recordLogin(null, request.getUsername(), "PASSWORD", "FAILURE",
                    loginIp, userAgent, "用户名或密码错误");
            throw BusinessException.of(401, "用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            loginLogService.recordLogin(user.getId(), user.getUsername(), "PASSWORD", "FAILURE",
                    loginIp, userAgent, "账号已被禁用");
            throw BusinessException.of(403, "账号已被禁用，请联系管理员");
        }

        // 验证密码（BCrypt）
        if (!userService.verifyPassword(request.getPassword(), user.getPassword())) {
            loginLogService.recordLogin(user.getId(), user.getUsername(), "PASSWORD", "FAILURE",
                    loginIp, userAgent, "用户名或密码错误");
            throw BusinessException.of(401, "用户名或密码错误");
        }

        // 生成Token
        String token = jwtUtil.generateToken(user.getUsername());

        // 获取用户权限
        java.util.List<String> permissions = new java.util.ArrayList<>(
                permissionService.getUserPermissionCodes(user.getId()));

        // 获取用户角色
        java.util.List<com.logistics.track17.entity.Role> roles = roleService.getRolesByUserId(user.getId());

        // 更新最后登录信息
        userService.updateLastLogin(user.getId(), loginIp);

        // 记录登录日志
        loginLogService.recordLogin(user.getId(), user.getUsername(), "PASSWORD", "SUCCESS",
                loginIp, userAgent, null);

        // 创建在线会话
        onlineSessionService.createSession(user.getId(), user.getUsername(), token, loginIp, userAgent, "PASSWORD");

        LoginResponse response = new LoginResponse(token, user.getUsername(), user.getRealName(), user.getAvatar(),
                expiration, permissions, roles);

        log.info("User {} logged in successfully from IP: {}", user.getUsername(), loginIp);
        return Result.success("登录成功", response);
    }

    /**
     * 用户登出 - 将 Token 加入黑名单
     */
    @PostMapping("/logout")
    public Result<Object> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = jwtUtil.getUsernameFromToken(token);

            // 移除在线会话
            onlineSessionService.removeSession(token);
            // 加入黑名单
            jwtUtil.blacklistToken(token);

            // 记录登出日志
            if (username != null) {
                User user = userService.getUserByUsername(username);
                loginLogService.recordLogin(user != null ? user.getId() : null, username, "LOGOUT", "SUCCESS",
                        getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);
            }
            log.info("User {} logged out", username);
        }
        return Result.success("登出成功", null);
    }

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
    public Result<UserDTO> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        // 从Authorization header中提取token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw BusinessException.of(401, "未授权");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);

        if (username == null) {
            throw BusinessException.of(401, "无效的token");
        }

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
        String key = DINGTALK_STATE_PREFIX + state;
        redisTemplate.opsForValue().set(key, Boolean.TRUE, DINGTALK_STATE_EXPIRE_SECONDS, TimeUnit.SECONDS);
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
            if (state == null || state.isBlank()) {
                httpResponse.sendRedirect(buildDingtalkErrorRedirect("缺少或无效的state参数"));
                return;
            }
            String key = DINGTALK_STATE_PREFIX + state;
            Object storedState = redisTemplate.opsForValue().get(key);
            if (storedState == null) {
                httpResponse.sendRedirect(buildDingtalkErrorRedirect("state已过期或无效"));
                return;
            }
            // 清理 state
            redisTemplate.delete(key);

            // 由于前端使用的是嵌入式二维码 (DTFrameLogin)，JS SDK 已经拿到了 authCode，
            // 并会通过发送 POST 请求来进行实际登录。
            // 为了防止本 GET 请求在重定向时提前消耗了 authCode（导致前端报 400 Bad Request），
            // 我们在这里中止 OAuth 的服务器端闭环，把它全部交给前端的 POST 接口去处理。

            httpResponse.setContentType("text/html;charset=utf-8");
            httpResponse.getWriter().write(
                    "<!DOCTYPE html><html><head><title>DingTalk Auth</title></head><body>" +
                            "<script>console.log('DingTalk auth code received by iframe, ignoring on backend to prevent double consumption.');</script>"
                            +
                            "</body></html>");

        } catch (Exception e) {
            log.error("DingTalk OAuth callback failed", e);
            httpResponse.sendRedirect(buildDingtalkErrorRedirect("钉钉登录失败: " + e.getMessage()));
        }
    }

    private String buildDingtalkErrorRedirect(String errorMessage) {
        String normalizedBase = normalizeFrontendBase();
        String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        return normalizedBase + "/login?error=" + encodedMessage;
    }

    private String normalizeFrontendBase() {
        return dingtalkFrontendRedirect.endsWith("/")
                ? dingtalkFrontendRedirect.substring(0, dingtalkFrontendRedirect.length() - 1)
                : dingtalkFrontendRedirect;
    }

    /**
     * 钉钉登录回调（POST请求 - 兼容前端直接调用）
     */
    @PostMapping("/dingtalk/callback")
    public Result<LoginResponse> dingTalkCallback(
            @org.springframework.validation.annotation.Validated @RequestBody com.logistics.track17.dto.DingTalkLoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("DingTalk login callback with code: {}", request.getCode());
        String loginIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            // 1. 用authCode换取accessToken
            String accessToken = dingTalkService.getAccessToken(request.getCode());

            // 2. 获取用户信息
            com.logistics.track17.dto.DingTalkUserInfo userInfo = dingTalkService.getUserInfo(accessToken);

            // 3. 验证CorpId（这里简化处理，使用配置的CorpId）
            if (!dingTalkService.validateCorpId(dingTalkConfig.getCorpId())) {
                loginLogService.recordLogin(null, null, "DINGTALK", "FAILURE",
                        loginIp, userAgent, "非本企业用户");
                throw com.logistics.track17.exception.BusinessException.of(403, "非本企业用户，禁止登录");
            }

            // 4. 登录或注册用户
            User user = userService.loginOrRegisterWithDingTalk(userInfo, dingTalkConfig.getCorpId());

            // 5. 生成JWT Token
            String token = jwtUtil.generateToken(user.getUsername());

            // 6. 获取用户权限和角色
            java.util.List<String> permissions = new java.util.ArrayList<>(
                    permissionService.getUserPermissionCodes(user.getId()));
            java.util.List<com.logistics.track17.entity.Role> roles = roleService.getRolesByUserId(user.getId());

            // 7. 更新最后登录信息
            userService.updateLastLogin(user.getId(), loginIp);

            // 8. 记录登录日志
            loginLogService.recordLogin(user.getId(), user.getUsername(), "DINGTALK", "SUCCESS",
                    loginIp, userAgent, null);

            // 9. 创建在线会话
            onlineSessionService.createSession(user.getId(), user.getUsername(), token, loginIp, userAgent, "DINGTALK");

            LoginResponse response = new LoginResponse(token, user.getUsername(), user.getRealName(), user.getAvatar(),
                    expiration, permissions, roles);

            log.info("User {} logged in via DingTalk from IP: {}", user.getUsername(), loginIp);
            return Result.success("登录成功", response);

        } catch (Exception e) {
            log.error("DingTalk login failed", e);
            loginLogService.recordLogin(null, null, "DINGTALK", "FAILURE",
                    loginIp, userAgent, e.getMessage());
            throw com.logistics.track17.exception.BusinessException.of("钉钉登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean isTrustedProxy = "127.0.0.1".equals(remoteAddr)
                || "0:0:0:0:0:0:0:1".equals(remoteAddr)
                || (remoteAddr != null && remoteAddr.startsWith("172."))
                || (remoteAddr != null && remoteAddr.startsWith("10."));

        if (isTrustedProxy) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
                return xff.contains(",") ? xff.split(",")[0].trim() : xff.trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isEmpty() && !"unknown".equalsIgnoreCase(realIp)) {
                return realIp.trim();
            }
        }
        return remoteAddr;
    }
}
