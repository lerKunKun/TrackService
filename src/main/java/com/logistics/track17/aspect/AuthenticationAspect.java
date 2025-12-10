package com.logistics.track17.aspect;

import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.entity.User;
import com.logistics.track17.exception.BusinessException;
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

    @Autowired
    private HttpServletRequest request;

    @Value("${security.aop.enabled:true}")
    private Boolean aopEnabled;

    // 排除路径列表（不需要鉴权的路径）
    private static final String[] EXCLUDE_PATHS = { "/api/v1/auth/", "/public/", "/health" };

    public AuthenticationAspect(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
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

        // 2. 检查是否是排除路径
        if (isExcludedPath(path)) {
            log.debug("Excluded path accessed: {}", path);
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

        // 5. 获取用户名并设置到请求属性
        String username = jwtUtil.getUsernameFromToken(token);
        request.setAttribute("username", username);

        // 6. 检查方法级别的权限注解
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequireAuth requireAuth = method.getAnnotation(RequireAuth.class);

        if (requireAuth != null) {
            User user = userService.getUserByUsername(username);

            if (user == null) {
                throw new BusinessException(401, "用户不存在");
            }

            // 检查管理员权限
            if (requireAuth.admin() && !"ADMIN".equals(user.getRole())) {
                throw new BusinessException(403, "需要管理员权限");
            }

            // 检查角色权限
            if (requireAuth.roles().length > 0) {
                boolean hasRole = Arrays.asList(requireAuth.roles()).contains(user.getRole());
                if (!hasRole) {
                    throw new BusinessException(403, "权限不足");
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
}
