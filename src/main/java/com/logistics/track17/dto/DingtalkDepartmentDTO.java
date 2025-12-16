package com.logistics.track17.dto;

import lombok.Data;

/**
 * 钉钉部门DTO
 * 对应钉钉开放平台返回的部门数据结构
 */
@Data
public class DingtalkDepartmentDTO {
    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 排序值
     */
    private Integer order;

    /**
     * 是否同步创建一个关联此部门的企业群
     */
    private Boolean createDeptGroup;

    /**
     * 是否默认同意加入该部门的申请
     */
    private Boolean autoAddUser;
}
