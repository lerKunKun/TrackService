package com.logistics.track17.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解
 * 用于标记需要记录审计日志的方法
 * 
 * 使用示例：
 * 
 * @AuditLog(operation = "创建用户", module = "用户管理")
 *                     public Result createUser() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 操作类型（必填）
     * 例如："创建用户"、"删除角色"、"修改权限"
     */
    String operation();

    /**
     * 模块名称
     * 例如："用户管理"、"角色管理"、"权限管理"
     */
    String module() default "";

    /**
     * 是否记录请求参数（默认记录）
     */
    boolean logParams() default true;

    /**
     * 是否记录返回结果（默认不记录）
     * 注意：可能包含敏感信息，谨慎开启
     */
    boolean logResult() default false;
}
