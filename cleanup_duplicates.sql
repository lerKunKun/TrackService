-- 清理重复的 theme_name 记录，保留最新的
DELETE FROM liquid_schema_cache
WHERE id NOT IN (
    SELECT * FROM (
        SELECT MAX(id)
        FROM liquid_schema_cache
        GROUP BY theme_name
    ) as temp
);

-- 重新添加唯一索引
ALTER TABLE liquid_schema_cache ADD UNIQUE KEY uk_file (theme_name(255));
