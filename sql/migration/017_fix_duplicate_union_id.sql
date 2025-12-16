-- Fix duplicate ding_union_id users
-- Keep the earliest created record, soft delete the rest

-- Find and soft delete duplicate records
UPDATE users u1
JOIN (
    SELECT ding_union_id, MIN(id) as keep_id
    FROM users
    WHERE ding_union_id IS NOT NULL 
      AND deleted_at IS NULL
    GROUP BY ding_union_id
    HAVING COUNT(*) > 1
) u2 ON u1.ding_union_id = u2.ding_union_id
SET u1.deleted_at = NOW()
WHERE u1.id != u2.keep_id 
  AND u1.deleted_at IS NULL;

-- Verify results
SELECT 
    u.id,
    u.username,
    u.ding_union_id,
    u.created_at,
    u.deleted_at
FROM users u
WHERE u.ding_union_id = 'B0nO6LsaueRNOBzC4w0PYQiEiE'
ORDER BY u.created_at;
