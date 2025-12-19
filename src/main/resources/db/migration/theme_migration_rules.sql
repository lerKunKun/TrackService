-- 新架构数据库表

-- 表1: 主题迁移规则表
CREATE TABLE IF NOT EXISTS theme_migration_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    theme_name VARCHAR(100) NOT NULL COMMENT '主题名称',
    from_version VARCHAR(50) NOT NULL COMMENT '源版本号',
    to_version VARCHAR(50) NOT NULL COMMENT '目标版本号',
    rule_type VARCHAR(50) NOT NULL COMMENT '规则类型: SECTION_RENAME, FIELD_MAPPING, DEFAULT_VALUE',
    section_name VARCHAR(100) COMMENT 'Section名称（对于字段映射和默认值）',
    rule_json TEXT NOT NULL COMMENT 'JSON格式的规则详情',
    confidence VARCHAR(20) COMMENT '置信度: CONFIRMED, HIGH, MEDIUM, LOW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by VARCHAR(100) COMMENT '创建人',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_rule (theme_name, from_version, to_version, rule_type, section_name),
    INDEX idx_version (theme_name, from_version, to_version),
    INDEX idx_type (rule_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主题迁移规则表';

-- 表2: Liquid Schema缓存表
CREATE TABLE IF NOT EXISTS liquid_schema_cache (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    theme_name VARCHAR(100) NOT NULL COMMENT '主题名称',
    version VARCHAR(50) NOT NULL COMMENT '版本号',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径，如: sections/hero.liquid',
    section_name VARCHAR(100) COMMENT 'Schema中的name字段',
    section_type VARCHAR(100) COMMENT 'Section类型标识',
    schema_json TEXT NOT NULL COMMENT '完整的schema JSON',
    settings_count INT DEFAULT 0 COMMENT 'Settings数量',
    settings_hash VARCHAR(64) COMMENT 'Settings的MD5哈希，用于快速比对',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_file (theme_name, version, file_path),
    INDEX idx_theme_version (theme_name, version),
    INDEX idx_section_name (section_name),
    INDEX idx_hash (settings_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Liquid Schema缓存表';
