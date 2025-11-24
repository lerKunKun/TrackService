-- =====================================================
-- 初始化管理员账号脚本
-- =====================================================
-- 使用说明:
-- 1. 确保已经执行了 database.sql 创建了 users 表
-- 2. 执行此脚本创建默认管理员账号
-- 3. 默认用户名: admin
-- 4. 默认密码: admin123
-- 5. 建议登录后立即修改密码
-- =====================================================

USE logistics_system;

-- 删除可能存在的旧管理员账号（仅用于重新初始化）
DELETE FROM `users` WHERE username = 'admin';

-- 插入默认管理员账号
-- 密码 admin123 的 BCrypt 哈希值
INSERT INTO `users` (
    `username`,
    `password`,
    `email`,
    `real_name`,
    `role`,
    `status`
) VALUES (
    'admin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2B.0A6VhNcVzLv0GNTD8VQ2',
    'admin@track17.com',
    '系统管理员',
    'ADMIN',
    1
);

-- 验证插入结果
SELECT id, username, email, real_name, role, status, created_at
FROM `users`
WHERE username = 'admin';

-- =====================================================
-- 提示信息
-- =====================================================
-- 管理员账号已创建！
-- 用户名: admin
-- 密码: admin123
--
-- 重要提示:
-- 1. 请立即登录并修改默认密码
-- 2. 不要在生产环境使用默认密码
-- 3. 定期更新密码以确保安全
-- =====================================================
