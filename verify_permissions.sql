-- Verify admin user role assignments and permissions
SELECT 'Admin Users and Their Roles:' as '';
SELECT 
    u.id, 
    u.username, 
    u.real_name,
    r.role_code,
    r.role_name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.id IN (1, 193)
ORDER BY u.id, r.id;

SELECT '' as '';
SELECT 'Permission Counts by Role:' as '';
SELECT 
    r.id,
    r.role_code,
    r.role_name,
    COUNT(rp.permission_id) as permission_count,
    (SELECT COUNT(*) FROM permissions) as total_permissions
FROM roles r
LEFT JOIN role_permissions rp ON r.id = rp.role_id
WHERE r.role_code = 'SUPER_ADMIN'
GROUP BY r.id, r.role_code, r.role_name;
