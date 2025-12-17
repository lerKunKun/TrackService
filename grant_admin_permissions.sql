-- Check current role assignments for admin users
SELECT 
    u.id, 
    u.username, 
    u.real_name,
    u.role as user_role_field,
    GROUP_CONCAT(r.role_code) as assigned_roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.id IN (1, 193)
GROUP BY u.id, u.username, u.real_name, u.role;

-- Assign SUPER_ADMIN role (id=1) to both admin users if not already assigned
INSERT IGNORE INTO user_roles (user_id, role_id, created_at, updated_at)
VALUES 
    (1, 1, NOW(), NOW()),      -- admin -> SUPER_ADMIN role
    (193, 1, NOW(), NOW());    -- superadmin -> SUPER_ADMIN role

-- Verify the assignment
SELECT 
    u.id, 
    u.username,
    r.role_code,
    r.role_name,
    (SELECT COUNT(*) FROM role_permissions rp WHERE rp.role_id = r.id) as permission_count
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.id IN (1, 193);
