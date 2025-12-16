-- 清理钉钉同步产生的重复用户
-- 模式：每个钉钉用户有两条记录
-- 1. 原始记录: 如 "01005056180126287801"
-- 2. 重复记录: 如 "01005056180126287801_1765849088066"

-- 步骤1：查看重复模式
SELECT 
    CASE 
        WHEN username LIKE '%\\_%' THEN SUBSTRING_INDEX(username, '_', 1)
        ELSE username 
    END as base_id,
    COUNT(*) as record_count,
    GROUP_CONCAT(username ORDER BY username) as all_usernames
FROM users 
WHERE deleted_at IS NULL
GROUP BY base_id
HAVING COUNT(*) > 1
ORDER BY record_count DESC
LIMIT 20;

-- 步骤2：统计要删除的记录数
SELECT 
    COUNT(*) as records_to_delete
FROM users
WHERE deleted_at IS NULL
  AND username LIKE '%\\_%'  -- 只删除带下划线的（重复的）
  AND LENGTH(SUBSTRING_INDEX(username, '_', -1)) = 13;  -- 确保最后部分是13位时间戳

-- 步骤3：软删除重复记录（保留原始记录，删除带时间戳的）
UPDATE users
SET deleted_at = NOW()
WHERE deleted_at IS NULL
  AND username LIKE '%\\_%'
  AND LENGTH(SUBSTRING_INDEX(username, '_', -1)) = 13
  AND EXISTS (
      -- 确保存在对应的原始记录
      SELECT 1
      FROM (SELECT username as orig_username FROM users WHERE deleted_at IS NULL) u2
      WHERE u2.orig_username = SUBSTRING_INDEX(users.username, '_', 1)
  );

-- 步骤4：验证清理结果
SELECT 
    '清理完成' as status,
    (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL) as active_users,
    (SELECT COUNT(*) FROM users WHERE deleted_at IS NOT NULL) as deleted_users;

-- 步骤5：检查是否还有重复
SELECT 
    COUNT(*) as remaining_duplicates
FROM (
    SELECT 
        CASE 
            WHEN username LIKE '%\\_%' THEN SUBSTRING_INDEX(username, '_', 1)
            ELSE username 
        END as base_id
    FROM users 
    WHERE deleted_at IS NULL
    GROUP BY base_id
    HAVING COUNT(*) > 1
) t;
