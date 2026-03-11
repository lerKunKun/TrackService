package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 邀请实体
 * 支持通过邮箱邀请用户加入公司/店铺
 */
@Data
public class Invitation {

    private Long id;

    /**
     * 公司 ID
     */
    private Long companyId;

    /**
     * 店铺 ID（可选，null 表示仅邀请加入公司）
     */
    private Long shopId;

    /**
     * 被邀请人邮箱
     */
    private String email;

    /**
     * 分配的角色 ID
     */
    private Long roleId;

    /**
     * 邀请 Token（唯一，用于邀请链接）
     */
    private String token;

    /**
     * 状态：PENDING / ACCEPTED / EXPIRED / REVOKED
     */
    private String status;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 邀请人 user_id
     */
    private Long invitedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
