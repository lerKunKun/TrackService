-- 业务权限数据补充
-- 版本: 017
-- 创建时间: 2025-12-17
-- 说明: 为业务功能添加细粒度权限控制

-- ========================================
-- 1. 插入业务权限
-- ========================================
INSERT INTO `permissions` (`permission_name`, `permission_code`, `permission_type`, `description`) VALUES
-- 店铺管理权限
('店铺查看', 'shop:view', 'MENU', '查看店铺列表'),
('店铺创建', 'shop:create', 'BUTTON', '创建新店铺'),
('店铺编辑', 'shop:update', 'BUTTON', '编辑店铺信息'),
('店铺删除', 'shop:delete', 'BUTTON', '删除店铺'),
('店铺授权', 'shop:oauth', 'BUTTON', 'Shopify店铺授权'),

-- 订单管理权限
('订单查看', 'order:view', 'MENU', '查看订单列表'),
('订单导出', 'order:export', 'BUTTON', '导出订单数据'),
('订单同步', 'order:sync', 'BUTTON', '手动同步订单'),
('订单详情', 'order:detail', 'BUTTON', '查看订单详情'),

-- 运单管理权限
('运单查看', 'tracking:view', 'MENU', '查看运单列表'),
('运单创建', 'tracking:create', 'BUTTON', '创建运单'),
('运单编辑', 'tracking:update', 'BUTTON', '编辑运单'),
('运单删除', 'tracking:delete', 'BUTTON', '删除运单'),
('运单追踪', 'tracking:track', 'BUTTON', '追踪运单状态'),

-- 钉钉同步权限
('钉钉同步执行', 'dingtalk:sync', 'BUTTON', '执行钉钉同步'),
('钉钉配置', 'dingtalk:config', 'BUTTON', '配置钉钉同步规则'),
('钉钉日志', 'dingtalk:log', 'BUTTON', '查看钉钉同步日志'),

-- 企业CorpID管理权限
('企业白名单查看', 'corp:view', 'BUTTON', '查看白名单企业列表'),
('企业白名单管理', 'corp:manage', 'BUTTON', '添加/删除白名单企业')

ON DUPLICATE KEY UPDATE `permission_name` = VALUES(`permission_name`);

-- ========================================
-- 2. 为ADMIN角色分配业务权限（除了删除权限）
-- ========================================
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 
    (SELECT id FROM roles WHERE role_code = 'ADMIN') as role_id,
    p.id as permission_id
FROM permissions p
WHERE p.permission_code IN (
    -- 店铺管理（不含删除）
    'shop:view', 'shop:create', 'shop:update', 'shop:oauth',
    
    -- 订单管理（全部）
    'order:view', 'order:export', 'order:sync', 'order:detail',
    
    -- 运单管理（不含删除）
    'tracking:view', 'tracking:create', 'tracking:update', 'tracking:track',
    
    -- 钉钉同步（全部）
    'dingtalk:sync', 'dingtalk:config', 'dingtalk:log',
    
    -- 企业白名单（全部）
    'corp:view', 'corp:manage'
)
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- ========================================
-- 3. 为USER角色分配基础权限（仅查看）
-- ========================================
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 
    (SELECT id FROM roles WHERE role_code = 'USER') as role_id,
    p.id as permission_id
FROM permissions p
WHERE p.permission_code IN (
    -- 仅查看权限
    'shop:view',
    'order:view', 'order:detail',
    'tracking:view', 'tracking:track'
)
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- ========================================
-- 4. 为SUPER_ADMIN角色分配所有新增权限
-- ========================================
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 
    (SELECT id FROM roles WHERE role_code = 'SUPER_ADMIN') as role_id,
    p.id as permission_id
FROM permissions p
WHERE p.permission_code LIKE 'shop:%'
   OR p.permission_code LIKE 'order:%'
   OR p.permission_code LIKE 'tracking:%'
   OR p.permission_code LIKE 'dingtalk:%'
   OR p.permission_code LIKE 'corp:%'
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- ========================================
-- 5. 验证权限分配
-- ========================================
-- 查看各角色的权限数量
-- SELECT 
--     r.role_name,
--     r.role_code,
--     COUNT(rp.permission_id) as permission_count
-- FROM roles r
-- LEFT JOIN role_permissions rp ON r.id = rp.role_id
-- GROUP BY r.id
-- ORDER BY r.id;

-- 查看ADMIN角色的所有权限
-- SELECT 
--     p.permission_name,
--     p.permission_code,
--     p.permission_type
-- FROM permissions p
-- INNER JOIN role_permissions rp ON p.id = rp.permission_id
-- INNER JOIN roles r ON rp.role_id = r.id
-- WHERE r.role_code = 'ADMIN'
-- ORDER BY p.permission_type, p.permission_code;
