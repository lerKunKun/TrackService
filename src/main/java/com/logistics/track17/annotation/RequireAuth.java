package com.logistics.track17.annotation;

import java.lang.annotation.*;

/**
 * 权限注解 - 需要登录认证
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAuth {

    /**
     * 是否需要管理员权限（兼容旧代码）
     * 检查用户是否拥有 ADMIN 或 SUPER_ADMIN 角色
     */
    boolean admin() default false;

    /**
     * 允许的角色列表（兼容旧代码）
     * 检查用户是否拥有指定的角色编码
     */
    String[] roles() default {};

    /**
     * 需要的权限码列表（推荐使用）
     * 例如: {"system:user:create", "system:user:update"}
     */
    String[] permissions() default {};

    /**
     * 权限验证模式
     * OR: 用户只需拥有其中任意一个权限即可通过（默认）
     * AND: 用户必须同时拥有所有权限才能通过
     */
    PermissionMode permissionMode() default PermissionMode.OR;

    /**
     * 权限验证模式枚举
     */
    enum PermissionMode {
        /**
         * OR 逻辑：拥有任一权限即可（默认，向后兼容）
         */
        OR,

        /**
         * AND 逻辑：必须拥有所有权限
         */
        AND
    }
}
