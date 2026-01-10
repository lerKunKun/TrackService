-- ========================================
-- P0 优化：迁移用户角色数据（简化版）
-- 版本: 019
-- 创建时间: 2026-01-07
-- 说明: 将 users.role 字段数据迁移到 user_roles 表
-- ========================================

-- 1. 迁移数据：将 users.role 映射到 user_roles 表
INSERT INTO `user_roles` (`user_id`, `role_id`)
SELECT 
    u.id as user_id,
    r.id as role_id
FROM users u
INNER JOIN roles r ON r.role_code = u.role COLLATE utf8mb4_0900_ai_ci
WHERE u.role IS NOT NULL
  AND u.role != ''
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur 
      WHERE ur.user_id = u.id 
        AND ur.role_id = r.id
  )
  AND u.deleted_at IS NULL;  -- 只迁移未软删除的用户

-- 2. 处理没有角色的用户，默认分配 USER 角色
INSERT INTO `user_roles` (`user_id`, `role_id`)
SELECT 
    u.id as user_id,
    (SELECT id FROM roles WHERE role_code = 'USER' LIMIT 1) as role_id
FROM users u
WHERE (u.role IS NULL OR u.role = '')
  AND u.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id
  );

-- 3. 统计迁移结果
SELECT 
    '迁移统计' as report_type,
    COUNT(DISTINCT u.id) as total_users,
    COUNT(DISTINCT ur.user_id) as users_with_roles,
    COUNT(DISTINCT ur.id) as total_role_assignments
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.deleted_at IS NULL;
