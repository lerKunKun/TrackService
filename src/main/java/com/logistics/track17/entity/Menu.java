package com.logistics.track17.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单实体类
 */
@Data
public class Menu {
    /**
     * 菜单ID
     */
    private Long id;

    /**
     * 父菜单ID（0为顶级菜单）
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 菜单编码（唯一标识）
     */
    private String menuCode;

    /**
     * 菜单类型：MENU-菜单，BUTTON-按钮
     */
    private String menuType;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 排序（越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 是否显示：0-隐藏，1-显示
     */
    private Integer visible;

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

    /**
     * 子菜单列表（树形结构，不持久化到数据库）
     * 只有当children不为空时才在JSON中显示
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Menu> children;
}
