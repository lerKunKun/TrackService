package com.logistics.track17.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解（服务层过滤方案）
 * 
 * 该注解用于标记需要数据权限控制的方法，提示开发者在实现时考虑数据范围过滤。
 * 注意：本注解仅作为标记和文档说明，实际权限过滤需要在代码中手动实现。
 * 
 * 使用示例：
 * 
 * <pre>
 * {@code @DataPermission(scope = DataScope.SELF, description = "只能查看自己创建的订单")
 * public List<Order> getMyOrders() {
 *     Long userId = UserContextHolder.getCurrentUserId();
 *     return orderMapper.selectByUserId(userId);
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 数据权限范围
     */
    DataScope scope() default DataScope.SELF;

    /**
     * 描述信息
     */
    String description() default "";

    /**
     * 用户ID字段名（用于文档说明）
     */
    String userIdColumn() default "user_id";

    /**
     * 部门ID字段名（用于文档说明）
     */
    String deptIdColumn() default "dept_id";

    /**
     * 数据权限范围枚举
     */
    enum DataScope {
        /**
         * 全部数据权限（需要特殊权限）
         */
        ALL("全部数据"),

        /**
         * 部门数据权限
         */
        DEPT("本部门数据"),

        /**
         * 部门及子部门数据权限
         */
        DEPT_AND_CHILD("本部门及下级部门数据"),

        /**
         * 仅本人数据权限
         */
        SELF("仅本人数据"),

        /**
         * 自定义数据权限（需要自行实现过滤逻辑）
         */
        CUSTOM("自定义");

        private final String description;

        DataScope(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
