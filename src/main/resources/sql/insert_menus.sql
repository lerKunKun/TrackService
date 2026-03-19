SET NAMES utf8mb4;

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, sort_order, visible, status) VALUES
(5, '操作日志', 'system-audit-logs', 'MENU', '/system/audit-logs', 'system/AuditLogs', 'FileSearchOutlined', 80, 1, 1),
(5, '登录日志', 'system-login-logs', 'MENU', '/system/login-logs', 'system/LoginLogs', 'LoginOutlined', 81, 1, 1),
(5, '在线用户', 'system-online-sessions', 'MENU', '/system/online-sessions', 'system/OnlineSessions', 'WifiOutlined', 82, 1, 1);

SET @audit_id = (SELECT id FROM menus WHERE menu_code = 'system-audit-logs');
SET @login_id = (SELECT id FROM menus WHERE menu_code = 'system-login-logs');
SET @online_id = (SELECT id FROM menus WHERE menu_code = 'system-online-sessions');

INSERT INTO role_menus (role_id, menu_id) VALUES
(1, @audit_id), (1, @login_id), (1, @online_id),
(2, @audit_id), (2, @login_id), (2, @online_id);
