-- 增加批量操作所需的权限
-- 1. 确保 product:delete, product:update, product:shops:update 权限存在
INSERT IGNORE INTO permissions (permission_name, permission_code, permission_type, description, status, created_at, updated_at) VALUES 
('删除产品', 'product:delete', 'BUTTON', '允许删除产品数据', 1, NOW(), NOW()),
('更新产品', 'product:update', 'BUTTON', '允许更新产品基本信息', 1, NOW(), NOW()),
('更新产品商店', 'product:shops:update', 'BUTTON', '允许更新产品和商店的关联', 1, NOW(), NOW());

-- 2. 将这些权限授予 admin 角色 (假设 admin 角色ID为 1)
-- 获取 admin 角色ID (通常是 1, 但为了安全起见, 我们用子查询)
SET @admin_role_id = (SELECT id FROM roles WHERE role_code = 'admin' LIMIT 1);
SET @super_admin_role_id = (SELECT id FROM roles WHERE role_code = 'superadmin' LIMIT 1);
SET @perm_delete_id = (SELECT id FROM permissions WHERE permission_code = 'product:delete');
SET @perm_update_id = (SELECT id FROM permissions WHERE permission_code = 'product:update');
SET @perm_shops_id = (SELECT id FROM permissions WHERE permission_code = 'product:shops:update');

-- 插入 admin 权限 (使用 INSERT IGNORE 避免重复)
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (@admin_role_id, @perm_delete_id);
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (@admin_role_id, @perm_update_id);
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (@admin_role_id, @perm_shops_id);

-- 插入 superadmin 权限 (如果有区分)
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (@super_admin_role_id, @perm_delete_id);
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (@super_admin_role_id, @perm_update_id);
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (@super_admin_role_id, @perm_shops_id);
