-- Quick migration commands (copy and paste into MySQL client)
-- Connect to your database first: mysql -u root -p logistics_system

USE logistics_system;

-- 1. Add deleted_at to users table
ALTER TABLE users 
ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
ADD INDEX idx_deleted_at (deleted_at);

-- 2. Add deleted_at to shops table
ALTER TABLE shops 
ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
ADD INDEX idx_deleted_at (deleted_at);

-- 3. Add deleted_at to tracking_numbers table
ALTER TABLE tracking_numbers 
ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
ADD INDEX idx_deleted_at (deleted_at);

-- Verify the changes
DESCRIBE users;
DESCRIBE shops;
DESCRIBE tracking_numbers;
