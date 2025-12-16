package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 钉钉部门映射实体
 * 记录钉钉部门与系统部门的映射关系
 */
@Data
public class DingtalkDeptMapping {
    /**
     * ID
     */
    private Long id;

    /**
     * 钉钉部门ID
     */
    private Long dingtalkDeptId;

    /**
     * 系统部门ID
     */
    private Long systemDeptId;

    /**
     * 钉钉部门名称（冗余字段，便于查询）
     */
    private String dingtalkDeptName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
