package com.logistics.track17.aspect;

import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.entity.Role;
import com.logistics.track17.entity.Shop;
import com.logistics.track17.entity.User;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.mapper.ShopMapper;
import com.logistics.track17.service.PermissionService;
import com.logistics.track17.service.RoleService;
import com.logistics.track17.service.UserService;
import com.logistics.track17.util.JwtUtil;
import com.logistics.track17.util.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证切面 - AOP权限控制
 * 
 * 职责：
 * 1. JWT Token 验证
 * 2. 解析 X-Shop-Id header → 设置 RequestContext（userId + companyId + shopId）
 * 3. @RequireAuth 注解的角色/权限检查
 * 4. 请求结束后清理 RequestContext
 */
@Aspect
@Component
@Slf4j
@Order(1)
public class AuthenticationAspect {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final PermissionService permissionService;
    private final RoleService roleService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private ShopMapper shopMapper;

    @Value("${security.aop.enabled:true}")
    private Boolean aopEnabled;

    // 排除路径列表（不需要鉴权的路径）
    private static final String[] EXCLUDE_PATHS = {
            "/api/v1/auth/",
            "/api/v1/oauth/",
            "/api/v1/webhooks/",
            "/api/v1/dingtalk/sync/",
            "/public/",
            "/health"
    };

    public AuthenticationAspect(JwtUtil jwtUtil,
            UserService userService,
            PermissionService permissionService,
            RoleService roleService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.permissionService = permissionService;
        this.roleService = roleService;
    }

    /**
     * 拦截所有Controller方法
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object checkAuthentication(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return doCheckAuthentication(joinPoint);
        } finally {
            // 确保始终清理 RequestContext，防止 ThreadLocal 内存泄漏
            RequestContext.clear();
        }
    }

    private Object doCheckAuthentication(ProceedingJoinPoint joinPoint) throws Throwable {
        // AOP未启用，直接放行
        if (!aopEnabled) {
            return joinPoint.proceed();
        }

        // 1. 获取请求路径
        String path = request.getRequestURI();
        log.debug("AOP拦截请求路径: {}", path);

        // 2. 检查是否是排除路径
        if (isExcludedPath(path)) {
            return joinPoint.proceed();
        }

        // 3. 获取并验证Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或Token已过期");
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "Token无效或已过期");
        }

        // 4. 获取用户信息
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        // 5. 设置 RequestContext（userId）
        RequestContext.init(user.getId());

        // 6. 设置用户信息到 request 属性（向后兼容 UserContextHolder）
        request.setAttribute("username", username);
        request.setAttribute("userId", user.getId());

        // 7. 解析 X-Shop-Id header → 设置 companyId + shopId
        resolveShopContext();

        // 8. 检查方法级别的权限注解
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequireAuth requireAuth = method.getAnnotation(RequireAuth.class);

        if (requireAuth != null) {
            // 8.1 检查管理员权限
            if (requireAuth.admin()) {
                if (!isAdminUser(user)) {
                    log.warn("User {} attempted to access admin-only resource: {}", username, path);
                    throw new BusinessException(403, "需要管理员权限");
                }
            }

            // 8.2 检查角色权限
            if (requireAuth.roles().length > 0) {
                if (!hasAnyRole(user, requireAuth.roles())) {
                    log.warn("User {} lacks required roles {} for resource: {}",
                            username, Arrays.toString(requireAuth.roles()), path);
                    throw new BusinessException(403, "权限不足：缺少所需角色");
                }
            }

            // 8.3 检查权限码
            if (requireAuth.permissions().length > 0) {
                if (!checkPermissions(user.getId(), requireAuth.permissions(), requireAuth.permissionMode())) {
                    log.warn("User {} lacks required permissions {} (mode: {}) for resource: {}",
                            username, Arrays.toString(requireAuth.permissions()),
                            requireAuth.permissionMode(), path);
                    throw new BusinessException(403, "权限不足：缺少所需权限");
                }
            }
        }

        log.debug("User {} accessed {} [companyId={}, shopId={}]",
                username, path, RequestContext.getCurrentCompanyId(), RequestContext.getCurrentShopId());
        return joinPoint.proceed();
    }

    /**
     * 解析 X-Shop-Id header，查询店铺的 companyId 并设置到 RequestContext
     */
    private void resolveShopContext() {
        String shopIdHeader = request.getHeader("X-Shop-Id");
        if (shopIdHeader == null || shopIdHeader.isEmpty()) {
            return;
        }

        try {
            Long shopId = Long.parseLong(shopIdHeader);
            // 查询店铺获取 companyId
            Shop shop = shopMapper.selectById(shopId);
            if (shop != null) {
                RequestContext.setCompanyAndShop(shop.getCompanyId(), shopId);
            } else {
                log.warn("X-Shop-Id {} 对应的店铺不存在", shopId);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid X-Shop-Id header: {}", shopIdHeader);
        }
    }

    private boolean isExcludedPath(String path) {
        for (String excludePath : EXCLUDE_PATHS) {
            if (path.startsWith(excludePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查用户是否是管理员
     */
    private boolean isAdminUser(User user) {
        List<Role> roles = roleService.getRolesByUserId(user.getId());
        return roles.stream()
                .map(Role::getRoleCode)
                .anyMatch(code -> "ADMIN".equals(code) || "SUPER_ADMIN".equals(code));
    }

    /**
     * 检查用户是否拥有任一指定角色
     */
    private boolean hasAnyRole(User user, String[] requiredRoles) {
        Set<String> requiredRoleSet = Arrays.stream(requiredRoles).collect(Collectors.toSet());
        List<Role> userRoles = roleService.getRolesByUserId(user.getId());
        return userRoles.stream()
                .map(Role::getRoleCode)
                .anyMatch(requiredRoleSet::contains);
    }

    /**
     * 检查用户是否拥有所需权限
     */
    private boolean checkPermissions(Long userId, String[] requiredPermissions, RequireAuth.PermissionMode mode) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return true;
        }

        Set<String> userPermissionCodes = permissionService.getUserPermissionCodes(userId);

        if (mode == RequireAuth.PermissionMode.AND) {
            return Arrays.stream(requiredPermissions).allMatch(userPermissionCodes::contains);
        } else {
            return Arrays.stream(requiredPermissions).anyMatch(userPermissionCodes::contains);
        }
    }
}
