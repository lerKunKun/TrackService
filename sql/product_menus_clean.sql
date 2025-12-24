-- 产品管理菜单数据 - UTF8编码
-- 使用chcp 65001设置PowerShell为UTF-8

INSERT INTO menus (menu_code, menu_name, menu_type, parent_id, path, icon, sort_order) VALUES ('product', '产品管理', 'MENU', 0, '/product', 'AppstoreOutlined', 5);
INSERT INTO menus (menu_code, menu_name, menu_type, parent_id, path, icon, sort_order) VALUES ('product-development', '产品开发', 'MENU', (SELECT id FROM (SELECT id FROM menus WHERE menu_code='product') t), '/product/development', 'ExperimentOutlined', 1);
INSERT INTO menus (menu_code, menu_name, menu_type, parent_id, path, icon, sort_order) VALUES ('product-procurement', '采购管理', 'MENU', (SELECT id FROM (SELECT id FROM menus WHERE menu_code='product') t), '/product/procurement', 'ShoppingCartOutlined', 2);
INSERT INTO menus (menu_code, menu_name, menu_type, parent_id, path, icon, sort_order) VALUES ('product-listing', '产品刊登', 'MENU', (SELECT id FROM (SELECT id FROM menus WHERE menu_code='product') t), '/product/listing', 'FileTextOutlined', 3);
INSERT INTO role_menus (role_id, menu_id) SELECT 1, id FROM menus WHERE menu_code IN ('product', 'product-development', 'product-procurement', 'product-listing');
SELECT menu_code, menu_name FROM menus WHERE menu_code LIKE 'product%';
