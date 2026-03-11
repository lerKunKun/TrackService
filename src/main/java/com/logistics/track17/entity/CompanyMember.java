package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公司成员实体
 * 表示用户在公司中的角色关系
 */
@Data
public class CompanyMember {

    private Long id;

    /**
     * 公司 ID
     */
    private Long companyId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 角色 ID（公司级角色：SUPER_ADMIN / ADMIN / MEMBER 等）
     */
    private Long roleId;

    /**
     * 邀请人 user_id
     */
    private Long invitedBy;

    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;

    /**
     * 状态：0-待确认，1-已加入，2-已离开
     */
    private Integer status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
