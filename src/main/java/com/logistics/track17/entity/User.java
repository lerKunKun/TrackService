package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
public class User {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（BCrypt加密）
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 角色：ADMIN-管理员，USER-普通用户
     * 
     * @deprecated 该字段已废弃，请使用 RBAC 系统的 user_roles 表进行角色管理
     * @see com.logistics.track17.entity.Role
     * @see com.logistics.track17.service.RoleService
     */
    @Deprecated
    private String role;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 是否启用自动同步：0-禁用同步，1-启用同步
     */
    private Integer syncEnabled;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 钉钉unionId（跨企业唯一标识）
     */
    private String dingUnionId;

    /**
     * 钉钉userId（企业内唯一标识）
     */
    private String dingUserId;

    /**
     * 工号
     */
    private String jobNumber;

    /**
     * 职位
     */
    private String title;

    /**
     * 企业CorpId
     */
    private String corpId;

    /**
     * 登录来源：PASSWORD, DINGTALK
     */
    private String loginSource;

    /**
     * 软删除时间戳
     */
    private LocalDateTime deletedAt;
}
