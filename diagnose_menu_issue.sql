-- 检查当前菜单数据
SELECT * FROM menus ORDER BY parent_id, sort_order;

-- 检查用户角色关联
SELECT u.id, u.username, r.id as role_id, r.role_name 
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.id = 1 OR u.id = 193;

-- 检查角色菜单关联
SELECT r.id, r.role_name, COUNT(rm.menu_id) as menu_count
FROM roles r
LEFT JOIN role_menus rm ON r.id = rm.role_id
GROUP BY r.id, r.role_name;

-- 测试getUserMenuTree的SQL查询
SELECT DISTINCT m.*
FROM menus m
INNER JOIN role_menus rm ON m.id = rm.menu_id
INNER JOIN user_roles ur ON rm.role_id = ur.role_id
WHERE ur.user_id = 1  -- 替换为实际用户ID
  AND m.status = 1
  AND m.visible = 1
ORDER BY m.parent_id, m.sort_order;
