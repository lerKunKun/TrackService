-- =====================================================
-- 钉钉同步功能相关表
-- 创建时间：2025-12-15
-- =====================================================

-- 1. 钉钉部门映射表
CREATE TABLE IF NOT EXISTS `dingtalk_dept_mapping` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `dingtalk_dept_id` BIGINT NOT NULL COMMENT '钉钉部门ID',
  `system_dept_id` BIGINT NOT NULL COMMENT '系统部门ID',
  `dingtalk_dept_name` VARCHAR(100) COMMENT '钉钉部门名称（冗余）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dingtalk_dept_id` (`dingtalk_dept_id`),
  KEY `idx_system_dept_id` (`system_dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钉钉部门映射表';

-- 2. 角色映射规则表
CREATE TABLE IF NOT EXISTS `role_mapping_rules` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `rule_type` VARCHAR(20) NOT NULL COMMENT '规则类型：DEPT-按部门，TITLE-按职位，DEPT_TITLE-部门+职位',
  `dingtalk_dept_id` BIGINT COMMENT '钉钉部门ID（rule_type=DEPT时使用）',
  `dingtalk_title` VARCHAR(100) COMMENT '钉钉职位名称（rule_type=TITLE时使用）',
  `system_role_id` BIGINT NOT NULL COMMENT '系统角色ID',
  `priority` INT DEFAULT 0 COMMENT '优先级（数字越大优先级越高）',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_system_role_id` (`system_role_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色映射规则表';

-- 3. 同步日志表
CREATE TABLE IF NOT EXISTS `dingtalk_sync_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `sync_type` VARCHAR(20) NOT NULL COMMENT '同步类型：DEPT-部门，USER-用户，FULL-全量',
  `sync_mode` VARCHAR(20) NOT NULL COMMENT '同步模式：MANUAL-手动，AUTO-自动',
  `status` VARCHAR(20) NOT NULL COMMENT '状态：SUCCESS-成功，FAILED-失败，PARTIAL-部分成功',
  `total_count` INT DEFAULT 0 COMMENT '总数',
  `success_count` INT DEFAULT 0 COMMENT '成功数',
  `failed_count` INT DEFAULT 0 COMMENT '失败数',
  `error_message` TEXT COMMENT '错误信息',
  `started_at` DATETIME COMMENT '开始时间',
  `completed_at` DATETIME COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sync_type` (`sync_type`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钉钉同步日志表';

-- 4. 修改 users 表：新增钉钉相关字段和同步控制字段
-- 检查字段是否存在，如果不存在则添加
SET @COLUMN_CHECK = (
  SELECT COUNT(*) 
  FROM INFORMATION_SCHEMA.COLUMNS 
  WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'ding_userid'
);

SET @SQL_ADD_DING_USERID = IF(
  @COLUMN_CHECK = 0,
  'ALTER TABLE users ADD COLUMN `ding_userid` VARCHAR(100) COMMENT ''钉钉userId（企业内唯一）'' AFTER `ding_union_id`',
  'SELECT ''Column ding_userid already exists'' AS Info'
);
PREPARE stmt FROM @SQL_ADD_DING_USERID;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 job_number 字段
SET @COLUMN_CHECK = (
  SELECT COUNT(*) 
  FROM INFORMATION_SCHEMA.COLUMNS 
  WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'job_number'
);

SET @SQL_ADD_JOB_NUMBER = IF(
  @COLUMN_CHECK = 0,
  'ALTER TABLE users ADD COLUMN `job_number` VARCHAR(50) COMMENT ''工号'' AFTER `ding_userid`',
  'SELECT ''Column job_number already exists'' AS Info'
);
PREPARE stmt FROM @SQL_ADD_JOB_NUMBER;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 title 字段
SET @COLUMN_CHECK = (
  SELECT COUNT(*) 
  FROM INFORMATION_SCHEMA.COLUMNS 
  WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'title'
);

SET @SQL_ADD_TITLE = IF(
  @COLUMN_CHECK = 0,
  'ALTER TABLE users ADD COLUMN `title` VARCHAR(100) COMMENT ''职位'' AFTER `job_number`',
  'SELECT ''Column title already exists'' AS Info'
);
PREPARE stmt FROM @SQL_ADD_TITLE;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 sync_enabled 字段
SET @COLUMN_CHECK = (
  SELECT COUNT(*) 
  FROM INFORMATION_SCHEMA.COLUMNS 
  WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'sync_enabled'
);

SET @SQL_ADD_SYNC_ENABLED = IF(
  @COLUMN_CHECK = 0,
  'ALTER TABLE users ADD COLUMN `sync_enabled` TINYINT DEFAULT 1 COMMENT ''是否启用自动同步：0-禁用同步，1-启用同步'' AFTER `status`',
  'SELECT ''Column sync_enabled already exists'' AS Info'
);
PREPARE stmt FROM @SQL_ADD_SYNC_ENABLED;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引
SET @INDEX_CHECK = (
  SELECT COUNT(*) 
  FROM INFORMATION_SCHEMA.STATISTICS 
  WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND INDEX_NAME = 'idx_ding_userid'
);

SET @SQL_ADD_INDEX = IF(
  @INDEX_CHECK = 0,
  'ALTER TABLE users ADD KEY `idx_ding_userid` (`ding_userid`)',
  'SELECT ''Index idx_ding_userid already exists'' AS Info'
);
PREPARE stmt FROM @SQL_ADD_INDEX;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为现有用户设置默认 sync_enabled 值
UPDATE users SET sync_enabled = 1 WHERE sync_enabled IS NULL AND deleted_at IS NULL;
