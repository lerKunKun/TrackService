-- V12: 添加通知配置菜单和权限
-- Date: 2026-02-13

-- 1. 获取系统管理父菜单ID
SET @systemParentId = (SELECT id FROM menus WHERE menu_code = 'system' LIMIT 1);

-- 2. 插入通知配置菜单
INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, sort_order, visible, status, created_at, updated_at)
SELECT
    @systemParentId,
    '通知配置',
    'alert-settings',
    'MENU',
    '/system/alert-settings',
    'system/AlertSettings',
    'BellOutlined',
    15,
    1,
    1,
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'alert-settings');

-- 3. 获取新菜单ID
SET @alertMenuId = (SELECT id FROM menus WHERE menu_code = 'alert-settings' LIMIT 1);

-- 4. 分配给SUPER_ADMIN角色
SET @superAdminRoleId = (SELECT id FROM roles WHERE role_code = 'SUPER_ADMIN' LIMIT 1);

INSERT INTO role_menus (role_id, menu_id)
SELECT @superAdminRoleId, @alertMenuId
FROM DUAL
WHERE @superAdminRoleId IS NOT NULL
AND @alertMenuId IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM role_menus WHERE role_id = @superAdminRoleId AND menu_id = @alertMenuId);

-- 5. 添加通知配置相关权限
INSERT INTO permissions (permission_name, permission_code, permission_type, description, created_at, updated_at)
SELECT '通知配置管理', 'system:alert:manage', 'BUTTON', '管理通知接收人和监控邮箱配置', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'system:alert:manage');

-- 6. 获取权限ID并分配给SUPER_ADMIN
SET @alertPermId = (SELECT id FROM permissions WHERE permission_code = 'system:alert:manage' LIMIT 1);

INSERT INTO role_permissions (role_id, permission_id)
SELECT @superAdminRoleId, @alertPermId
FROM DUAL
WHERE @superAdminRoleId IS NOT NULL
AND @alertPermId IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = @superAdminRoleId AND permission_id = @alertPermId);
