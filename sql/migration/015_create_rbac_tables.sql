-- RBAC权限和菜单管理系统 - 数据库表结构
-- 版本: 015
-- 创建时间: 2025-12-16

-- ========================================
-- 1. 菜单表
-- ========================================
CREATE TABLE IF NOT EXISTS `menus` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `parent_id` BIGINT UNSIGNED NULL DEFAULT 0 COMMENT '父菜单ID（0为顶级菜单）',
    `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
    `menu_code` VARCHAR(50) NOT NULL COMMENT '菜单编码（唯一标识）',
    `menu_type` VARCHAR(20) NOT NULL COMMENT '菜单类型：MENU-菜单，BUTTON-按钮',
    `path` VARCHAR(200) NULL COMMENT '路由路径',
    `component` VARCHAR(200) NULL COMMENT '组件路径',
    `icon` VARCHAR(50) NULL COMMENT '菜单图标',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序（越小越靠前）',
    `visible` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否显示：0-隐藏，1-显示',
    `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_menu_code` (`menu_code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- ========================================
-- 2. 权限表
-- ========================================
CREATE TABLE IF NOT EXISTS `permissions` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码（唯一标识）',
    `permission_type` VARCHAR(20) NOT NULL COMMENT '权限类型：MENU-菜单，BUTTON-按钮，DATA-数据',
    `resource_type` VARCHAR(50) NULL COMMENT '资源类型',
    `resource_id` BIGINT NULL COMMENT '资源ID（如菜单ID）',
    `description` VARCHAR(200) NULL COMMENT '权限描述',
    `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ========================================
-- 3. 角色表
-- ========================================
CREATE TABLE IF NOT EXISTS `roles` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码（唯一标识）',
    `description` VARCHAR(200) NULL COMMENT '角色描述',
    `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ========================================
-- 4. 角色-菜单关联表
-- ========================================
CREATE TABLE IF NOT EXISTS `role_menus` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
    `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ========================================
-- 5. 角色-权限关联表
-- ========================================
CREATE TABLE IF NOT EXISTS `role_permissions` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
    `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ========================================
-- 6. 用户-角色关联表
-- ========================================
CREATE TABLE IF NOT EXISTS `user_roles` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ========================================
-- 7. 修改钉钉角色映射规则表，关联新的角色表
-- ========================================
ALTER TABLE `role_mapping_rules`
    CHANGE `system_role_id` `role_id` BIGINT UNSIGNED NOT NULL COMMENT '系统角色ID（关联roles表）';

-- ========================================
-- 8. 插入默认角色
-- ========================================
INSERT INTO `roles` (`role_name`, `role_code`, `description`, `status`) VALUES
('超级管理员', 'SUPER_ADMIN', '拥有系统所有权限，不可删除', 1),
('管理员', 'ADMIN', '系统管理员，拥有大部分管理权限', 1),
('普通用户', 'USER', '普通用户，拥有基本功能权限', 1)
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

-- ========================================
-- 9. 插入默认菜单
-- ========================================
INSERT INTO `menus` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `icon`, `sort_order`, `status`) VALUES
-- 一级菜单
(1, 0, '首页概览', 'dashboard', 'MENU', '/dashboard', '@/views/Dashboard.vue', 'DashboardOutlined', 1, 1),
(2, 0, '店铺管理', 'shops', 'MENU', '/shops', '@/views/Shops.vue', 'ShopOutlined', 2, 1),
(3, 0, '订单管理', 'orders', 'MENU', '/orders', '@/views/Orders.vue', 'ShoppingOutlined', 3, 1),
(4, 0, '运单管理', 'tracking', 'MENU', '/tracking', '@/views/Tracking.vue', 'CarOutlined', 4, 1),
(5, 0, '系统管理', 'system', 'MENU', '/system', NULL, 'SettingOutlined', 10, 1),

-- 系统管理子菜单
(51, 5, '用户管理', 'system-users', 'MENU', '/system/users', '@/views/Users.vue', 'TeamOutlined', 1, 1),
(52, 5, '角色管理', 'system-roles', 'MENU', '/system/roles', '@/views/system/Roles.vue', 'UserSwitchOutlined', 2, 1),
(53, 5, '菜单管理', 'system-menus', 'MENU', '/system/menus', '@/views/system/Menus.vue', 'MenuOutlined', 3, 1),
(54, 5, '权限管理', 'system-permissions', 'MENU', '/system/permissions', '@/views/system/Permissions.vue', 'SafetyOutlined', 4, 1),
(55, 5, '企业CorpID管理', 'system-corp-ids', 'MENU', '/system/allowed-corp-ids', '@/views/AllowedCorpIds.vue', 'IdcardOutlined', 5, 1),
(56, 5, '钉钉组织同步', 'system-dingtalk-sync', 'MENU', '/system/dingtalk-sync', '@/views/DingtalkSync.vue', 'SyncOutlined', 6, 1)
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`);

-- ========================================
-- 10. 插入默认权限
-- ========================================
INSERT INTO `permissions` (`permission_name`, `permission_code`, `permission_type`, `description`) VALUES
-- 用户管理权限
('用户查看', 'system:user:view', 'MENU', '查看用户列表'),
('用户创建', 'system:user:create', 'BUTTON', '创建新用户'),
('用户编辑', 'system:user:update', 'BUTTON', '编辑用户信息'),
('用户删除', 'system:user:delete', 'BUTTON', '删除用户'),

-- 角色管理权限
('角色查看', 'system:role:view', 'MENU', '查看角色列表'),
('角色创建', 'system:role:create', 'BUTTON', '创建新角色'),
('角色编辑', 'system:role:update', 'BUTTON', '编辑角色信息'),
('角色删除', 'system:role:delete', 'BUTTON', '删除角色'),
('角色分配权限', 'system:role:assign-permission', 'BUTTON', '为角色分配权限'),
('角色分配菜单', 'system:role:assign-menu', 'BUTTON', '为角色分配菜单'),

-- 菜单管理权限
('菜单查看', 'system:menu:view', 'MENU', '查看菜单列表'),
('菜单创建', 'system:menu:create', 'BUTTON', '创建新菜单'),
('菜单编辑', 'system:menu:update', 'BUTTON', '编辑菜单'),
('菜单删除', 'system:menu:delete', 'BUTTON', '删除菜单'),

-- 权限管理权限
('权限查看', 'system:permission:view', 'MENU', '查看权限列表'),
('权限创建', 'system:permission:create', 'BUTTON', '创建新权限'),
('权限编辑', 'system:permission:update', 'BUTTON', '编辑权限'),
('权限删除', 'system:permission:delete', 'BUTTON', '删除权限')
ON DUPLICATE KEY UPDATE `permission_name` = VALUES(`permission_name`);

-- ========================================
-- 11. 为超级管理员角色分配所有菜单
-- ========================================
INSERT INTO `role_menus` (`role_id`, `menu_id`)
SELECT 1, id FROM `menus` WHERE `status` = 1
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- ========================================
-- 12. 为超级管理员角色分配所有权限
-- ========================================
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 1, id FROM `permissions` WHERE `status` = 1
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);
