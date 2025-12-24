-- 使用UTF-8编码重新插入产品管理菜单
-- 先确保使用UTF-8连接
SET NAMES utf8mb4;

-- 1. 添加产品开发子菜单
INSERT INTO menus (menu_code, menu_name, menu_type, parent_id, path, icon, sort_order) 
VALUES ('product-development', '产品开发', 'MENU', 60, '/product/development', 'ExperimentOutlined', 1);

-- 2. 添加采购管理子菜单
INSERT INTO menus (menu_code, menu_name, menu_type, parent_id, path, icon, sort_order) 
VALUES ('product-procurement', '采购管理', 'MENU', 60, '/product/procurement', 'ShoppingCartOutlined', 2);

-- 3. 添加产品刊登子菜单
INSERT INTO menus (menu_code, menu_name, menu_type, parent_id, path, icon, sort_order) 
VALUES ('product-listing', '产品刊登', 'MENU', 60, '/product/listing', 'FileTextOutlined', 3);

-- 4. 为超级管理员分配菜单权限
INSERT INTO role_menus (role_id, menu_id) SELECT 1, 60;
INSERT INTO role_menus (role_id, menu_id) SELECT 1, id FROM menus WHERE menu_code = 'product-development';
INSERT INTO role_menus (role_id, menu_id) SELECT 1, id FROM menus WHERE menu_code = 'product-procurement';
INSERT INTO role_menus (role_id, menu_id) SELECT 1, id FROM menus WHERE menu_code = 'product-listing';

-- 5. 验证结果
SELECT id, menu_code, menu_name, parent_id, path FROM menus WHERE menu_code LIKE 'product%' ORDER BY parent_id, sort_order;
