INSERT INTO users (
    username, 
    password, 
    email, 
    real_name, 
    role,
    status, 
    sync_enabled,
    created_at,
    updated_at
) VALUES (
    'superadmin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z3j2b5HphsM2TBKoDDZxwZwK',
    'admin@example.com',
    'SuperAdmin',
    'ADMIN',
    1,
    0,
    NOW(),
    NOW()
);

SET @super_user_id = LAST_INSERT_ID();

INSERT INTO user_roles (user_id, role_id, created_at)
VALUES (@super_user_id, 1, NOW());

SELECT 
    u.id,
    u.username,
    u.real_name,
    u.email,
    u.status,
    r.role_name,
    r.role_code
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'superadmin';
