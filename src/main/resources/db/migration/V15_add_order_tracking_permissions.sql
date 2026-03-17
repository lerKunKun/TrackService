-- V15: 添加订单和运单权限码，补全数据访问控制

-- 1. 插入订单和运单权限
INSERT IGNORE INTO permissions (permission_name, permission_code, permission_type, description, status, created_at, updated_at)
VALUES
('查看订单', 'order:view', 'MENU', '允许查看订单列表和详情', 1, NOW(), NOW()),
('查看运单', 'tracking:view', 'MENU', '允许查看运单列表和详情', 1, NOW(), NOW()),
('管理运单', 'tracking:manage', 'BUTTON', '允许创建、编辑、删除、同步运单', 1, NOW(), NOW());

-- 2. 将这些权限授予 ADMIN 和 SUPER_ADMIN 角色
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
JOIN permissions p ON p.permission_code IN ('order:view', 'tracking:view', 'tracking:manage')
WHERE r.role_code IN ('ADMIN', 'SUPER_ADMIN');
