-- ========================================
-- P0 优化：添加用户管理权限
-- 版本: 018
-- 创建时间: 2026-01-07
-- ========================================

-- 1. 插入用户管理权限
INSERT INTO `permissions` (`permission_name`, `permission_code`, `permission_type`, `description`, `status`) VALUES
('用户查看', 'system:user:view', 'MENU', '查看用户列表和详情', 1),
('用户创建', 'system:user:create', 'BUTTON', '创建新用户', 1),
('用户编辑', 'system:user:update', 'BUTTON', '编辑用户信息、修改密码、更新状态', 1),
('用户删除', 'system:user:delete', 'BUTTON', '删除用户', 1)
ON DUPLICATE KEY UPDATE 
    `permission_name` = VALUES(`permission_name`),
    `description` = VALUES(`description`);

-- 2. 为 SUPER_ADMIN 角色分配所有用户管理权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 
    (SELECT id FROM roles WHERE role_code = 'SUPER_ADMIN') as role_id,
    id as permission_id
FROM permissions 
WHERE permission_code LIKE 'system:user:%'
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- 3. 为 ADMIN 角色分配所有用户管理权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 
    (SELECT id FROM roles WHERE role_code = 'ADMIN') as role_id,
    id as permission_id
FROM permissions 
WHERE permission_code LIKE 'system:user:%'
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- 4. 为 USER 角色分配查看权限（可以查看用户列表，但不能修改）
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 
    (SELECT id FROM roles WHERE role_code = 'USER') as role_id,
    id as permission_id
FROM permissions 
WHERE permission_code = 'system:user:view'
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- 5. 为 USER 角色分配首页菜单权限
INSERT INTO `role_menus` (`role_id`, `menu_id`)
SELECT 
    (SELECT id FROM roles WHERE role_code = 'USER') as role_id,
    id as menu_id
FROM menus 
WHERE menu_code IN ('dashboard')
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- 6. 验证权限分配结果
SELECT 
    r.role_name,
    r.role_code,
    COUNT(DISTINCT rp.permission_id) as permission_count,
    COUNT(DISTINCT rm.menu_id) as menu_count,
    GROUP_CONCAT(DISTINCT p.permission_code ORDER BY p.permission_code SEPARATOR ', ') as permissions,
    GROUP_CONCAT(DISTINCT m.menu_name ORDER BY m.menu_name SEPARATOR ', ') as menus
FROM roles r
LEFT JOIN role_permissions rp ON r.id = rp.role_id
LEFT JOIN permissions p ON rp.permission_id = p.id
LEFT JOIN role_menus rm ON r.id = rm.role_id
LEFT JOIN menus m ON rm.menu_id = m.id
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN', 'USER')
GROUP BY r.id, r.role_name, r.role_code
ORDER BY r.id;
