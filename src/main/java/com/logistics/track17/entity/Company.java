package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公司/租户实体
 * 作为多租户数据隔离的顶层组织单元
 */
@Data
public class Company {

    private Long id;

    /**
     * 公司名称
     */
    private String name;

    /**
     * 公司编码（唯一标识，用于邀请链接等）
     */
    private String code;

    /**
     * 公司创建者/所有者 user_id
     */
    private Long ownerUserId;

    /**
     * 公司 Logo URL
     */
    private String logo;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
