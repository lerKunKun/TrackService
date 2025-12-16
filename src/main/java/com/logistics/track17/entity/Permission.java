package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 权限实体类
 */
@Data
public class Permission {
    /**
     * 权限ID
     */
    private Long id;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限编码（唯一标识）
     */
    private String permissionCode;

    /**
     * 权限类型：MENU-菜单，BUTTON-按钮，DATA-数据
     */
    private String permissionType;

    /**
     * 资源类型
     */
    private String resourceType;

    /**
     * 资源ID（如菜单ID）
     */
    private Long resourceId;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
