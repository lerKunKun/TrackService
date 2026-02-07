-- Add Product Listing and Authorization Menus
-- Version: V5_add_product_auth_menus
-- Date: 2026-02-03

-- 1. Get Parent Menu ID (Product Management)
-- Assuming 'product' is the code for the top-level Product menu
SET @parentId = (SELECT id FROM menus WHERE menu_code = 'product' LIMIT 1);

-- If parent not found, try to find by name or create a fallback (usually handled by initial seed)
-- For safety, if null, we might skip or fail. assuming it exists as other product menus exist.

-- 2. Insert Product Listing Menu
INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, sort_order, visible, status, created_at, updated_at)
SELECT 
    @parentId, 
    '产品刊登', 
    'product-listing', 
    'MENU', 
    '/product/listing', 
    'product/listing/ProductListing',
    'FileTextOutlined', 
    5, 
    1, 
    1, 
    NOW(), 
    NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'product-listing');

-- 3. Insert Product Authorization Menu
INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, sort_order, visible, status, created_at, updated_at)
SELECT 
    @parentId, 
    '产品可见性', 
    'product-authorization', 
    'MENU', 
    '/product/authorization', 
    'product/ProductAuthorization',
    'SafetyOutlined', 
    6, 
    1, 
    1, 
    NOW(), 
    NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'product-authorization');

-- 4. Get New Menu IDs
SET @listingMenuId = (SELECT id FROM menus WHERE menu_code = 'product-listing' LIMIT 1);
SET @authMenuId = (SELECT id FROM menus WHERE menu_code = 'product-authorization' LIMIT 1);

-- 5. Assign to Roles (Super Admin - usually ID 1, and Product Manager)
-- 5.1 Assign to Super Admin (Role ID 1)
INSERT INTO role_menus (role_id, menu_id)
SELECT 1, @listingMenuId
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM role_menus WHERE role_id = 1 AND menu_id = @listingMenuId);

INSERT INTO role_menus (role_id, menu_id)
SELECT 1, @authMenuId
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM role_menus WHERE role_id = 1 AND menu_id = @authMenuId);

-- 5.2 Assign to Product Manager (Find ID by code)
SET @prodMgrRoleId = (SELECT id FROM roles WHERE role_code = 'PRODUCT_MANAGER' LIMIT 1);

INSERT INTO role_menus (role_id, menu_id)
SELECT @prodMgrRoleId, @listingMenuId
FROM DUAL
WHERE @prodMgrRoleId IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM role_menus WHERE role_id = @prodMgrRoleId AND menu_id = @listingMenuId);

INSERT INTO role_menus (role_id, menu_id)
SELECT @prodMgrRoleId, @authMenuId
FROM DUAL
WHERE @prodMgrRoleId IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM role_menus WHERE role_id = @prodMgrRoleId AND menu_id = @authMenuId);

-- 5.3 Assign Product Listing to Shop Operator
SET @shopOpRoleId = (SELECT id FROM roles WHERE role_code = 'SHOP_OPERATOR' LIMIT 1);

INSERT INTO role_menus (role_id, menu_id)
SELECT @shopOpRoleId, @listingMenuId
FROM DUAL
WHERE @shopOpRoleId IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM role_menus WHERE role_id = @shopOpRoleId AND menu_id = @listingMenuId);
