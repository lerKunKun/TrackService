package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * User-Shop-Role Association Entity
 * Maps users to shops with specific roles for multi-shop management
 */
@Data
public class UserShopRole {

    /**
     * Primary key
     */
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Shop ID
     */
    private Long shopId;

    /**
     * Role ID (references roles table)
     */
    private Long roleId;

    /**
     * Who granted this permission
     */
    private Long grantedBy;

    /**
     * When permission was granted
     */
    private LocalDateTime grantedAt;

    /**
     * Permission expiration time (null means permanent)
     */
    private LocalDateTime expiresAt;

    /**
     * Created timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Updated timestamp
     */
    private LocalDateTime updatedAt;
}
