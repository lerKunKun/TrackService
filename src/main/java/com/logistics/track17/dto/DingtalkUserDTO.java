package com.logistics.track17.dto;

import lombok.Data;
import java.util.List;

/**
 * 钉钉用户DTO
 * 对应钉钉开放平台返回的用户数据结构
 */
@Data
public class DingtalkUserDTO {
    /**
     * 用户的userId
     */
    private String userid;

    /**
     * 用户在当前开发者企业账号范围内的唯一标识
     */
    private String unionid;

    /**
     * 员工姓名
     */
    private String name;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 所属部门ID列表
     */
    private List<Long> deptIdList;

    /**
     * 职位
     */
    private String title;

    /**
     * 工号
     */
    private String jobNumber;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 是否已激活
     */
    private Boolean active;

    /**
     * 是否为企业的管理员
     */
    private Boolean admin;

    /**
     * 是否为企业的老板
     */
    private Boolean boss;
}
