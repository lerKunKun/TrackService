-- 修复 liquid_schema_cache 表的唯一索引
-- 问题：当前 uk_file 只基于 theme_name，导致同一主题的不同版本无法插入
-- 解决：删除旧索引，创建基于 (theme_name, version, file_path) 的复合唯一索引

-- 1. 删除错误的唯一索引
ALTER TABLE liquid_schema_cache DROP INDEX uk_file;

-- 2. 创建正确的复合唯一索引
ALTER TABLE liquid_schema_cache 
ADD UNIQUE KEY uk_theme_version_file (theme_name(200), version, file_path(200));
