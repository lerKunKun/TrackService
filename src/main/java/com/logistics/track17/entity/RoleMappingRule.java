package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色映射规则实体
 * 配置钉钉部门/职位到系统角色的自动分配规则
 */
@Data
public class RoleMappingRule {
    /**
     * ID
     */
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型
     * DEPT - 按部门匹配
     * TITLE - 按职位匹配
     * DEPT_TITLE - 部门+职位匹配
     */
    private String ruleType;

    /**
     * 钉钉部门ID（ruleType=DEPT时使用）
     */
    private Long dingtalkDeptId;

    /**
     * 钉钉职位名称（ruleType=TITLE时使用）
     */
    private String dingtalkTitle;

    /**
     * 系统角色ID
     */
    private Long systemRoleId;

    /**
     * 优先级（数字越大优先级越高）
     */
    private Integer priority;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
