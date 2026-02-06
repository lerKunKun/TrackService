-- Multi-Shop Authorization Migration Script
-- Version: V4_multi_shop_authorization
-- Date: 2026-02-02

-- PART 1: Create user_shop_roles table
CREATE TABLE IF NOT EXISTS user_shop_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    granted_by BIGINT NULL,
    granted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_user_shop_role (user_id, shop_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_shop_id (shop_id),
    KEY idx_role_id (role_id),
    KEY idx_expires_at (expires_at),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- PART 2: Create product_visibility table
CREATE TABLE IF NOT EXISTS product_visibility (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    role_id BIGINT UNSIGNED NULL,
    shop_id BIGINT NULL,
    granted_by BIGINT NOT NULL,
    granted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_product_user_shop (product_id, user_id, shop_id),
    UNIQUE KEY uk_product_role_shop (product_id, role_id, shop_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id),
    KEY idx_product_id (product_id),
    KEY idx_shop_id (shop_id),
    KEY idx_expires_at (expires_at),
    KEY idx_product_visibility_lookup (product_id, user_id, role_id, shop_id),
    
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- PART 3: Enhance product_shops table (check if column exists first)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'product_shops' 
    AND COLUMN_NAME = 'published_by');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE product_shops ADD COLUMN published_by BIGINT NULL AFTER last_export_time, ADD KEY idx_published_by (published_by)',
    'SELECT "Column published_by already exists" AS Info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- PART 4: Create new roles
INSERT INTO roles (role_name, role_code, description, status, created_at) 
VALUES 
('Product Manager', 'PRODUCT_MANAGER', 'Manage product visibility authorization', 1, NOW()),
('Shop Operator', 'SHOP_OPERATOR', 'Publish products to shops', 1, NOW()),
('Shop Admin', 'SHOP_ADMIN', 'Full shop management permissions', 1, NOW())
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- PART 5: Create new permissions
INSERT INTO permissions (permission_name, permission_code, permission_type, resource_type, description, status, created_at)
VALUES 
('Manage Product Visibility', 'product:visibility:manage', 'BUTTON', 'PRODUCT', 'Grant/revoke product authorization', 1, NOW()),
('View Authorized Products', 'product:visibility:view', 'MENU', 'PRODUCT', 'View authorized product list', 1, NOW()),
('Publish Product', 'product:publish', 'BUTTON', 'PRODUCT', 'Publish product to shop', 1, NOW()),
('Unpublish Product', 'product:unpublish', 'BUTTON', 'PRODUCT', 'Unpublish product from shop', 1, NOW()),
('Manage Shop Permission', 'shop:permission:manage', 'BUTTON', 'SHOP', 'Assign user shop permissions', 1, NOW()),
('View Shop Data', 'shop:data:view', 'DATA', 'SHOP', 'View shop data permission', 1, NOW()),
('View All Shops', 'shop:all:view', 'MENU', 'SHOP', 'View all shops (admin only)', 1, NOW())
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- PART 6: Product Manager permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'PRODUCT_MANAGER'
AND p.permission_code IN (
    'product:visibility:manage',
    'product:visibility:view',
    'product:price:update',
    'shop:view'
);

-- PART 7: Shop Operator permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'SHOP_OPERATOR'
AND p.permission_code IN (
    'product:visibility:view',
    'product:publish',
    'product:unpublish',
    'shop:data:view'
);

-- PART 8: Shop Admin permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'SHOP_ADMIN'
AND (p.permission_code LIKE 'shop:%' 
     OR p.permission_code LIKE 'product:%' 
     OR p.permission_code LIKE 'order:%');

-- PART 9: Migrate shops.user_id to user_shop_roles
INSERT INTO user_shop_roles (user_id, shop_id, role_id, granted_by, granted_at)
SELECT 
    s.user_id,
    s.id,
    (SELECT id FROM roles WHERE role_code = 'SHOP_ADMIN' LIMIT 1) AS role_id,
    1 AS granted_by,
    NOW() AS granted_at
FROM shops s
WHERE s.user_id IS NOT NULL
ON DUPLICATE KEY UPDATE 
    granted_at = VALUES(granted_at);
