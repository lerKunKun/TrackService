-- V10: RBAC integrity repair and referential constraints

-- 1) Ensure required permission codes exist
INSERT IGNORE INTO permissions (permission_name, permission_code, permission_type, description, status, created_at, updated_at)
VALUES
('Delete Product', 'product:delete', 'BUTTON', 'Delete product records', 1, NOW(), NOW()),
('Update Product', 'product:update', 'BUTTON', 'Update product details', 1, NOW(), NOW()),
('Update Product Shops', 'product:shops:update', 'BUTTON', 'Update product-shop relations', 1, NOW(), NOW()),
('Update Product Price', 'product:price:update', 'BUTTON', 'Update product price', 1, NOW(), NOW()),
('Publish Product', 'product:publish', 'BUTTON', 'Publish product to shop', 1, NOW(), NOW()),
('Unpublish Product', 'product:unpublish', 'BUTTON', 'Unpublish product from shop', 1, NOW(), NOW()),
('Manage Product Visibility', 'product:visibility:manage', 'BUTTON', 'Manage product visibility', 1, NOW(), NOW()),
('View Product Visibility', 'product:visibility:view', 'BUTTON', 'View product visibility', 1, NOW(), NOW()),
('View Shop', 'shop:view', 'MENU', 'View shops', 1, NOW(), NOW()),
('Delete Shop', 'shop:delete', 'BUTTON', 'Delete shop', 1, NOW(), NOW()),
('Shop OAuth', 'shop:oauth', 'BUTTON', 'Manage shop oauth actions', 1, NOW(), NOW()),
('Manage Shop Permission', 'shop:permission:manage', 'BUTTON', 'Manage shop permissions', 1, NOW(), NOW());

-- 2) Grant baseline permissions to ADMIN and SUPER_ADMIN
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
JOIN permissions p ON p.permission_code IN (
    'system:role:assign',
    'system:role:assign-menu',
    'system:role:assign-permission',
    'product:delete',
    'product:update',
    'product:shops:update',
    'product:price:update',
    'product:publish',
    'product:unpublish',
    'product:visibility:manage',
    'product:visibility:view',
    'shop:view',
    'shop:delete',
    'shop:oauth',
    'shop:permission:manage'
)
WHERE r.role_code IN ('ADMIN', 'SUPER_ADMIN');

-- 3) Remove orphan RBAC relationships
DELETE ur
FROM user_roles ur
LEFT JOIN users u ON u.id = ur.user_id
LEFT JOIN roles r ON r.id = ur.role_id
WHERE u.id IS NULL OR r.id IS NULL;

DELETE rp
FROM role_permissions rp
LEFT JOIN roles r ON r.id = rp.role_id
LEFT JOIN permissions p ON p.id = rp.permission_id
WHERE r.id IS NULL OR p.id IS NULL;

DELETE rm
FROM role_menus rm
LEFT JOIN roles r ON r.id = rm.role_id
LEFT JOIN menus m ON m.id = rm.menu_id
WHERE r.id IS NULL OR m.id IS NULL;

-- 4) Backfill RBAC role for active users with no user_roles row
SET @role_super_admin = (SELECT id FROM roles WHERE role_code = 'SUPER_ADMIN' LIMIT 1);
SET @role_admin = (SELECT id FROM roles WHERE role_code = 'ADMIN' LIMIT 1);
SET @role_user = (SELECT id FROM roles WHERE role_code = 'USER' LIMIT 1);

INSERT IGNORE INTO user_roles (user_id, role_id, created_at)
SELECT
    u.id,
    CASE
        WHEN UPPER(u.role) = 'SUPER_ADMIN' AND @role_super_admin IS NOT NULL THEN @role_super_admin
        WHEN UPPER(u.role) = 'ADMIN' AND @role_admin IS NOT NULL THEN @role_admin
        ELSE @role_user
    END AS role_id,
    NOW()
FROM users u
LEFT JOIN user_roles ur ON ur.user_id = u.id
WHERE u.deleted_at IS NULL
  AND ur.user_id IS NULL
  AND @role_user IS NOT NULL;

-- 5) Sync legacy users.role field from RBAC result
UPDATE users u
SET u.role = CASE
    WHEN EXISTS (
        SELECT 1
        FROM user_roles ur
        JOIN roles r ON r.id = ur.role_id
        WHERE ur.user_id = u.id
          AND r.role_code IN ('ADMIN', 'SUPER_ADMIN')
    ) THEN 'ADMIN'
    ELSE 'USER'
END
WHERE u.deleted_at IS NULL;

-- 6) Align user_roles.user_id signedness with users.id so FK can be added
ALTER TABLE user_roles
MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '用户ID';

-- 7) Add missing foreign keys (idempotent)
SET @db_name = DATABASE();

SET @fk_cnt = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @db_name
      AND TABLE_NAME = 'user_roles'
      AND CONSTRAINT_NAME = 'fk_user_roles_user'
);
SET @sql = IF(@fk_cnt = 0,
    'ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_cnt = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @db_name
      AND TABLE_NAME = 'user_roles'
      AND CONSTRAINT_NAME = 'fk_user_roles_role'
);
SET @sql = IF(@fk_cnt = 0,
    'ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_cnt = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @db_name
      AND TABLE_NAME = 'role_permissions'
      AND CONSTRAINT_NAME = 'fk_role_permissions_role'
);
SET @sql = IF(@fk_cnt = 0,
    'ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_cnt = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @db_name
      AND TABLE_NAME = 'role_permissions'
      AND CONSTRAINT_NAME = 'fk_role_permissions_permission'
);
SET @sql = IF(@fk_cnt = 0,
    'ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_cnt = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @db_name
      AND TABLE_NAME = 'role_menus'
      AND CONSTRAINT_NAME = 'fk_role_menus_role'
);
SET @sql = IF(@fk_cnt = 0,
    'ALTER TABLE role_menus ADD CONSTRAINT fk_role_menus_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_cnt = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @db_name
      AND TABLE_NAME = 'role_menus'
      AND CONSTRAINT_NAME = 'fk_role_menus_menu'
);
SET @sql = IF(@fk_cnt = 0,
    'ALTER TABLE role_menus ADD CONSTRAINT fk_role_menus_menu FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
