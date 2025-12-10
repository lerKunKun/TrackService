-- =========================================
-- 软删除功能 - 添加 deleted_at 字段
-- =========================================
-- 执行时间: 2025-12-10
-- 目标: 将所有删除操作从硬删除改为软删除
-- =========================================

USE logistics_system;

-- 1. 为 users 表添加 deleted_at 字段
ALTER TABLE users 
ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
ADD INDEX idx_deleted_at (deleted_at);

-- 2. 为 shops 表添加 deleted_at 字段
ALTER TABLE shops 
ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
ADD INDEX idx_deleted_at (deleted_at);

-- 3. 为 tracking_numbers 表添加 deleted_at 字段
ALTER TABLE tracking_numbers 
ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
ADD INDEX idx_deleted_at (deleted_at);

-- =========================================
-- 验证修改
-- =========================================
-- 查看表结构
DESCRIBE users;
DESCRIBE shops;
DESCRIBE tracking_numbers;

-- 查看索引
SHOW INDEX FROM users WHERE Key_name = 'idx_deleted_at';
SHOW INDEX FROM shops WHERE Key_name = 'idx_deleted_at';
SHOW INDEX FROM tracking_numbers WHERE Key_name = 'idx_deleted_at';
