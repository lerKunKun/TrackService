package com.logistics.track17.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户数据传输对象（不包含密码）
 */
@Data
public class UserDTO {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

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
     * 角色
     */
    private String role;

    /**
     * 状态
     */
    private Integer status;

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
     * 钉钉unionId
     */
    private String dingUnionId;

    /**
     * 钉钉userId
     */
    private String dingUserId;
}
