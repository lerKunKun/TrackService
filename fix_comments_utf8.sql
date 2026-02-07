-- 设置客户端字符集为 utf8mb4
SET NAMES utf8mb4;

-- 修复 liquid_schema_cache 表注释
ALTER TABLE liquid_schema_cache COMMENT 'Liquid Schema缓存表';

-- 修复 theme_migration_rules 表注释  
ALTER TABLE theme_migration_rules COMMENT '主题迁移规则表';
