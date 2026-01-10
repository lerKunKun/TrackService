-- ========================================
-- P2-7 优化：创建审计日志表
-- 版本: 020
-- 创建时间: 2026-01-07
-- 说明: 审计日志表，记录用户的重要操作
-- ========================================

CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT UNSIGNED NULL COMMENT '操作用户ID',
    `username` VARCHAR(50) NULL COMMENT '操作用户名',
    `operation` VARCHAR(100) NOT NULL COMMENT '操作类型',
    `module` VARCHAR(50) NOT NULL COMMENT '模块名称',
    `method` VARCHAR(200) NULL COMMENT '方法名',
    `params` TEXT NULL COMMENT '请求参数（JSON格式）',
    `result` VARCHAR(20) NOT NULL COMMENT '操作结果：SUCCESS/FAILURE',
    `error_msg` TEXT NULL COMMENT '错误信息',
    `ip_address` VARCHAR(50) NULL COMMENT 'IP地址',
    `user_agent` VARCHAR(500) NULL COMMENT '用户代理',
    `execution_time` INT NULL COMMENT '执行时长（毫秒）',
    `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_operation` (`operation`),
    KEY `idx_module` (`module`),
    KEY `idx_result` (`result`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 插入初始说明数据
INSERT INTO `audit_logs` (`user_id`, `username`, `operation`, `module`, `method`, `result`, `ip_address`, `created_at`)
VALUES (NULL, 'system', '系统初始化', '审计日志', 'SQL Migration 020', 'SUCCESS', '127.0.0.1', NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;
