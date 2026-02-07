-- 修复权限名称乱码
UPDATE permissions SET permission_name = '分配角色', description = '给用户分配角色的权限' WHERE permission_code = 'system:role:assign';

-- 验证
SELECT id, permission_name, permission_code, description FROM permissions WHERE permission_code = 'system:role:assign';
