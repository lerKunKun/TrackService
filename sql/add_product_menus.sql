-- 添加产品管理子菜单和权限
-- product主菜单已存在(id=60)，现在添加子菜单和权限

-- 1. 添加产品开发子菜单
INSERT INTO menus (menu_code, menu_name, menu_type, parent_id, path, icon, sort_order, created_at, updated_at)
VALUES ('product-development', '产品开发', 'MENU', 60, '/product/development', 'ExperimentOutlined', 1, NOW(), NOW());

-- 2. 添加采购管理子菜单
INSERT INTO menus (menu_code, menu_name, menu_type, parent_id, path, icon, sort_order, created_at, updated_at)
VALUES ('product-procurement', '采购管理', 'MENU', 60, '/product/procurement', 'ShoppingCartOutlined', 2, NOW(), NOW());

-- 3. 添加产品刊登子菜单
INSERT INTO menus (menu_code, menu_name, menu_type, parent_id, path, icon, sort_order, created_at, updated_at)
VALUES ('product-listing', '产品刊登', 'MENU', 60, '/product/listing', 'FileTextOutlined', 3, NOW(), NOW());

-- 4. 为超级管理员分配product主菜单权限
INSERT INTO role_menus (role_id, menu_id, created_at)
SELECT 1, 60, NOW();

-- 5. 为超级管理员分配product-development权限
INSERT INTO role_menus (role_id, menu_id, created_at)
SELECT 1, id, NOW() FROM menus WHERE menu_code = 'product-development';

-- 6. 为超级管理员分配product-procurement权限
INSERT INTO role_menus (role_id, menu_id, created_at)
SELECT 1, id, NOW() FROM menus WHERE menu_code = 'product-procurement';

-- 7. 为超级管理员分配product-listing权限
INSERT INTO role_menus (role_id, menu_id, created_at)
SELECT 1, id, NOW() FROM menus WHERE menu_code = 'product-listing';

-- 8. 验证插入结果
SELECT 
  m.id,
  m.menu_code,
  m.menu_name,
  m.parent_id,
  m.path
FROM menus m
WHERE m.menu_code LIKE 'product%'
ORDER BY m.parent_id, m.sort_order;
