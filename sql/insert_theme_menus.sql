-- Insert theme management top-level menu
INSERT INTO `menus` (`menu_code`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `icon`, `sort_order`, `visible`, `status`) 
VALUES ('theme', 'Theme Management', 0, 'MENU', '/theme', NULL, 'FormatPainterOutlined', 60, 1, 1);

-- Get the parent menu ID
SET @parent_menu_id = (SELECT id FROM menus WHERE menu_code = 'theme');

-- Insert submenu: Version Management
INSERT INTO `menus` (`menu_code`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `icon`, `sort_order`, `visible`, `status`) 
VALUES ('theme-versions', 'Version Management', @parent_menu_id, 'MENU', '/theme/versions', 'theme/ThemeVersions', 'AppstoreOutlined', 1, 1, 1);

-- Insert submenu: Theme Migration
INSERT INTO `menus` (`menu_code`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `icon`, `sort_order`, `visible`, `status`) 
VALUES ('theme-migration', 'Theme Migration', @parent_menu_id, 'MENU', '/theme/migration', 'theme/ThemeMigration', 'SwapOutlined', 2, 1, 1);
