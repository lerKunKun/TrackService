-- 修复 liquid_schema_cache 表注释
ALTER TABLE liquid_schema_cache COMMENT 'Liquid Schema缓存表';

-- 修复 liquid_schema_cache 列注释
ALTER TABLE liquid_schema_cache 
  MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN theme_name varchar(500) NOT NULL COMMENT '主题名称',
  MODIFY COLUMN schema_json text NOT NULL COMMENT '完整的schema JSON',
  MODIFY COLUMN settings_count int DEFAULT 0 COMMENT 'Settings数量',
  MODIFY COLUMN settings_hash varchar(64) DEFAULT NULL COMMENT 'Settings的MD5哈希',
  MODIFY COLUMN created_at timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN updated_at timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 修复 theme_migration_rules 表注释
ALTER TABLE theme_migration_rules COMMENT '主题迁移规则表';

-- 修复 theme_migration_rules 列注释
ALTER TABLE theme_migration_rules
  MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN rule_type varchar(50) NOT NULL COMMENT '规则类型',
  MODIFY COLUMN rule_json text NOT NULL COMMENT 'JSON格式的规则详情',
  MODIFY COLUMN section_name varchar(100) DEFAULT NULL COMMENT '节段名称',
  MODIFY COLUMN confidence varchar(20) DEFAULT NULL COMMENT '置信度',
  MODIFY COLUMN created_at timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN created_by varchar(100) DEFAULT NULL COMMENT '创建人',
  MODIFY COLUMN updated_at timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
