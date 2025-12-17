package com.logistics.track17.aspect;

import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.entity.Permission;
import com.logistics.track17.entity.Role;
import com.logistics.track17.entity.User;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.service.PermissionService;
import com.logistics.track17.service.RoleService;
import com.logistics.track17.service.UserService;
import com.logistics.track17.util.JwtUtil;
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
 */
@Aspect
@Component
@Slf4j
@Order(1) // 优先级最高
public class AuthenticationAspect {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final PermissionService permissionService;
    private final RoleService roleService;

    @Autowired
    private HttpServletRequest request;

    @Value("${security.aop.enabled:true}")
    private Boolean aopEnabled;

    // 排除路径列表（不需要鉴权的路径）
    private static final String[] EXCLUDE_PATHS = {
            "/api/v1/auth/",
            "/api/v1/oauth/", // OAuth授权和回调路径
            "/api/v1/webhooks/", // Shopify Webhooks路径
            "/api/v1/dingtalk/sync/", // 钉钉同步接口
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
        // AOP未启用，直接放行
        if (!aopEnabled) {
            return joinPoint.proceed();
        }

        // 1. 获取请求路径
        String path = request.getRequestURI();
        log.info("AOP拦截请求路径: {}", path);

        // 2. 检查是否是排除路径
        if (isExcludedPath(path)) {
            log.info("白名单路径，直接放行: {}", path);
            return joinPoint.proceed();
        }

        // 3. 获取Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或Token已过期");
        }

        String token = authHeader.substring(7);

        // 4. 验证Token
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "Token无效或已过期");
        }

        // 5. 获取用户信息并设置到请求属性
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.getUserByUsername(username);

        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        // 设置用户信息到request属性（供Controller使用）
        request.setAttribute("username", username);
        request.setAttribute("userId", user.getId());

        // 6. 检查方法级别的权限注解
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequireAuth requireAuth = method.getAnnotation(RequireAuth.class);

        if (requireAuth != null) {
            // 6.1 检查管理员权限（兼容旧代码）
            if (requireAuth.admin()) {
                if (!isAdminUser(user)) {
                    log.warn("User {} attempted to access admin-only resource: {}", username, path);
                    throw new BusinessException(403, "需要管理员权限");
                }
            }

            // 6.2 检查角色权限（兼容旧代码，基于user.role字段或user_roles表）
            if (requireAuth.roles().length > 0) {
                if (!hasAnyRole(user, requireAuth.roles())) {
                    log.warn("User {} lacks required roles {} for resource: {}",
                            username, Arrays.toString(requireAuth.roles()), path);
                    throw new BusinessException(403, "权限不足：缺少所需角色");
                }
            }

            // 6.3 检查权限码（推荐使用，基于RBAC的细粒度权限控制）
            if (requireAuth.permissions().length > 0) {
                if (!hasAnyPermission(user.getId(), requireAuth.permissions())) {
                    log.warn("User {} lacks required permissions {} for resource: {}",
                            username, Arrays.toString(requireAuth.permissions()), path);
                    throw new BusinessException(403, "权限不足：缺少所需权限");
                }
            }
        }

        log.debug("User {} accessed {}", username, path);
        return joinPoint.proceed();
    }

    /**
     * 检查是否是排除路径
     */
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
     * 检查逻辑：
     * 1. 检查user.role字段是否为ADMIN（向后兼容）
     * 2. 检查user_roles表中是否有ADMIN或SUPER_ADMIN角色
     */
    private boolean isAdminUser(User user) {
        // 兼容旧代码：检查user.role字段
        if ("ADMIN".equals(user.getRole()) || "SUPER_ADMIN".equals(user.getRole())) {
            return true;
        }

        // 新RBAC系统：检查user_roles表
        List<Role> roles = roleService.getRolesByUserId(user.getId());
        return roles.stream()
                .map(Role::getRoleCode)
                .anyMatch(code -> "ADMIN".equals(code) || "SUPER_ADMIN".equals(code));
    }

    /**
     * 检查用户是否拥有任一指定角色
     * 检查逻辑：
     * 1. 检查user.role字段（向后兼容）
     * 2. 检查user_roles表（RBAC系统）
     */
    private boolean hasAnyRole(User user, String[] requiredRoles) {
        Set<String> requiredRoleSet = Arrays.stream(requiredRoles).collect(Collectors.toSet());

        // 兼容旧代码：检查user.role字段
        if (user.getRole() != null && requiredRoleSet.contains(user.getRole())) {
            return true;
        }

        // 新RBAC系统：检查user_roles表
        List<Role> userRoles = roleService.getRolesByUserId(user.getId());
        return userRoles.stream()
                .map(Role::getRoleCode)
                .anyMatch(requiredRoleSet::contains);
    }

    /**
     * 检查用户是否拥有任一指定权限
     * 基于RBAC系统，查询user_roles -> role_permissions -> permissions
     */
    private boolean hasAnyPermission(Long userId, String[] requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return true;
        }

        // 查询用户所有权限
        List<Permission> userPermissions = permissionService.getPermissionsByUserId(userId);
        Set<String> permissionCodes = userPermissions.stream()
                .map(Permission::getPermissionCode)
                .collect(Collectors.toSet());

        // 检查是否拥有任一所需权限（OR逻辑）
        return Arrays.stream(requiredPermissions)
                .anyMatch(permissionCodes::contains);
    }
}
