package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Product Visibility Authorization Entity
 * Controls which users or roles can view and publish specific products
 */
@Data
public class ProductVisibility {

    /**
     * Primary key
     */
    private Long id;

    /**
     * Product ID
     */
    private Long productId;

    /**
     * User ID (for individual user authorization, mutually exclusive with roleId)
     */
    private Long userId;

    /**
     * Role ID (for role-based authorization, mutually exclusive with userId)
     */
    private Long roleId;

    /**
     * Shop ID (optional, limits visibility to specific shop context)
     */
    private Long shopId;

    /**
     * Who granted this authorization
     */
    private Long grantedBy;

    /**
     * When authorization was granted
     */
    private LocalDateTime grantedAt;

    /**
     * Authorization expiration time (null means permanent)
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
