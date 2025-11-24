-- =========================================
-- 运单管理系统 - 乐观锁支持
-- =========================================
-- 执行时间: 2025-11-23
-- 目标: 添加版本号字段，解决并发更新问题
-- =========================================

USE logistics_system;

-- 1. 添加版本号字段
ALTER TABLE tracking_numbers
ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）' AFTER updated_at;

-- 2. 为现有数据初始化版本号（如果有数据）
UPDATE tracking_numbers SET version = 0 WHERE version IS NULL;

-- 3. 验证字段添加
DESC tracking_numbers;

-- =========================================
-- 使用说明
-- =========================================
-- 更新时需要检查版本号:
--
-- UPDATE tracking_numbers
-- SET remarks = 'xxx',
--     version = version + 1,
--     updated_at = NOW()
-- WHERE id = xxx
--   AND version = #{oldVersion}
--
-- 如果 affected rows = 0，说明版本冲突
-- =========================================
