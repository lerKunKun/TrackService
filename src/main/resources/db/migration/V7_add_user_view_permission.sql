-- Add view permissions to PRODUCT_MANAGER and SHOP_OPERATOR
-- Purpose: Allow these roles to see user/role lists when granting product visibility

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code IN ('PRODUCT_MANAGER', 'SHOP_OPERATOR')
AND p.permission_code IN ('system:user:view', 'system:role:view');
