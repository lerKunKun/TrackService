-- Theme Version Archive Table
CREATE TABLE IF NOT EXISTS `theme_version_archive` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `theme_name` VARCHAR(100) NOT NULL,
  `version` VARCHAR(20) NOT NULL,
  `zip_file_path` VARCHAR(500) NOT NULL,
  `zip_file_size` BIGINT(20),
  `sections_count` INT(11),
  `is_current` TINYINT(1) DEFAULT 0,
  `uploaded_by` VARCHAR(50),
  `uploaded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_theme_name (`theme_name`),
  INDEX idx_version (`version`),
  UNIQUE KEY uk_theme_version (`theme_name`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Theme Version Archive';

-- Theme Migration Rules Table
CREATE TABLE IF NOT EXISTS `theme_migration_rules` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `from_version` VARCHAR(20) NOT NULL,
  `to_version` VARCHAR(20) NOT NULL,
  `old_section` VARCHAR(100) NOT NULL,
  `new_section` VARCHAR(100) NOT NULL,
  `confidence` VARCHAR(20) DEFAULT 'MEDIUM',
  `rule_type` VARCHAR(20) DEFAULT 'AUTO',
  `created_by` VARCHAR(50),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_versions (`from_version`, `to_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Theme Migration Rules';

-- Theme Migration History Table
CREATE TABLE IF NOT EXISTS `theme_migration_history` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `theme_name` VARCHAR(100) NOT NULL,
  `from_version` VARCHAR(20) NOT NULL,
  `to_version` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) DEFAULT 'PENDING',
  `templates_updated` INT(11) DEFAULT 0,
  `executed_by` VARCHAR(50),
  `executed_at` DATETIME,
  `completed_at` DATETIME,
  `error_message` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_theme_name (`theme_name`),
  INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Theme Migration History';

-- Product Template Suffixes Table
CREATE TABLE IF NOT EXISTS `product_template_suffixes` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `theme_name` VARCHAR(100) NOT NULL,
  `suffix` VARCHAR(50) NOT NULL,
  `sections_snapshot` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_theme_suffix (`theme_name`, `suffix`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product Template Suffixes';

-- Theme Exports Table
CREATE TABLE IF NOT EXISTS `theme_exports` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `theme_name` VARCHAR(100) NOT NULL,
  `version` VARCHAR(20) NOT NULL,
  `export_path` VARCHAR(500) NOT NULL,
  `file_size` BIGINT(20),
  `exported_by` VARCHAR(50),
  `exported_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_theme_name (`theme_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Theme Exports';
