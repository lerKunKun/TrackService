-- 021_add_system_canvas_menu.sql
-- 添加系统画布菜单（使用UTF8MB4十六进制编码避免乱码）

-- 插入系统画布菜单到系统管理下 (parent_id = 5)
-- 系统画布 = E7B3BBE7BB9FE794BBE5B883 (UTF-8 hex)
INSERT IGNORE INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, sort_order, visible, status)
VALUES (5, X'E7B3BBE7BB9FE794BBE5B883', 'system-canvas', 'MENU', '/system/canvas', '/views/system/SystemCanvas.vue', 'ApartmentOutlined', 70, 1, 1);

-- 给超级管理员和管理员分配菜单权限
INSERT IGNORE INTO role_menus (role_id, menu_id)
SELECT 1, id FROM menus WHERE menu_code = 'system-canvas'
UNION ALL
SELECT 2, id FROM menus WHERE menu_code = 'system-canvas';

-- 验证结果
SELECT m.id, m.menu_name, m.menu_code, m.path,
       GROUP_CONCAT(r.role_code) as assigned_roles
FROM menus m
LEFT JOIN role_menus rm ON m.id = rm.menu_id
LEFT JOIN roles r ON rm.role_id = r.id
WHERE m.menu_code = 'system-canvas'
GROUP BY m.id;
