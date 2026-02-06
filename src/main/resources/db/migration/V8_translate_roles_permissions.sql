-- Translate roles
UPDATE roles SET role_name = '产品经理', description = '管理产品及授权' WHERE role_code = 'PRODUCT_MANAGER';
UPDATE roles SET role_name = '店铺运营', description = '负责店铺产品刊登' WHERE role_code = 'SHOP_OPERATOR';
UPDATE roles SET role_name = '店铺管理员', description = '店铺完全管理权限' WHERE role_code = 'SHOP_ADMIN';

-- Translate standard roles if they exist
UPDATE roles SET role_name = '超级管理员' WHERE role_code = 'SUPER_ADMIN';
UPDATE roles SET role_name = '管理员' WHERE role_code = 'ADMIN';
UPDATE roles SET role_name = '普通用户' WHERE role_code = 'USER';

-- Translate permissions
UPDATE permissions SET permission_name = '管理产品可见性', description = '分配/撤销产品授权' WHERE permission_code = 'product:visibility:manage';
UPDATE permissions SET permission_name = '查看授权产品', description = '查看被授权的产品' WHERE permission_code = 'product:visibility:view';
UPDATE permissions SET permission_name = '刊登产品', description = '将产品刊登到店铺' WHERE permission_code = 'product:publish';
UPDATE permissions SET permission_name = '取消刊登', description = '取消产品刊登' WHERE permission_code = 'product:unpublish';
UPDATE permissions SET permission_name = '管理店铺权限', description = '分配用户店铺权限' WHERE permission_code = 'shop:permission:manage';
UPDATE permissions SET permission_name = '查看店铺数据', description = '查看店铺数据权限' WHERE permission_code = 'shop:data:view';
UPDATE permissions SET permission_name = '查看所有店铺', description = '查看所有店铺（超管）' WHERE permission_code = 'shop:all:view';

-- Fix any potentially garbled comments or descriptions if needed (optional but good practice)
-- (Already handled by V6 generally, but updates here ensure new text is correct)
