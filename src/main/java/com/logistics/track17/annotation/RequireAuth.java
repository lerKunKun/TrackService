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
     * 是否需要管理员权限
     */
    boolean admin() default false;

    /**
     * 允许的角色列表
     */
    String[] roles() default {};
}
