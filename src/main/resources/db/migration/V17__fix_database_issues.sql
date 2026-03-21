-- ============================================================
-- V17__fix_database_issues.sql
-- 修复数据库潜在问题：Collation统一、冗余索引清理、缺失索引补充、
-- 软删除唯一约束、级联删除审查、慢查询日志、demo_table清理
--
-- 执行前注意事项：
--   1. 请先在测试环境验证
--   2. 大表 ALTER 会锁表，建议在低峰期执行
--   3. 建议逐段执行，确认无报错后再执行下一段
--   4. 执行前请备份数据库
-- ============================================================

-- ============================================================
-- 第1部分：统一表字符序 (Collation)
-- 将 28 张 utf8mb4_0900_ai_ci 表统一为 utf8mb4_unicode_ci
-- ============================================================

ALTER TABLE audit_logs CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE carriers CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE companies CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE company_members CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE email_monitor_config CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE fulfillments CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE invitations CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE liquid_schema_cache CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE login_logs CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE menus CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE notification_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE notification_recipient CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE order_items CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE orders CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE permissions CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE product_media CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE product_template CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE product_template_suffixes CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE role_menus CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE role_permissions CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE roles CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE theme_exports CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE theme_migration_history CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE theme_migration_rules CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE theme_version_archive CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE tracking_webhook_logs CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE user_roles CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- 第2部分：修复 users 表软删除与 UNIQUE 约束冲突
-- 改为复合唯一键，允许已删除用户的用户名/邮箱被重新注册
--
-- 原理：deleted_at 为 NULL 时，(username, NULL) 唯一
--        deleted_at 不为 NULL 时，(username, '2026-xx-xx') 各自唯一
-- ============================================================

-- 2.1 删除旧的唯一索引
ALTER TABLE users DROP INDEX uk_username;
ALTER TABLE users DROP INDEX uk_email;

-- 2.2 创建新的复合唯一索引（包含 deleted_at）
ALTER TABLE users ADD UNIQUE INDEX uk_username_alive (username, deleted_at);
ALTER TABLE users ADD UNIQUE INDEX uk_email_alive (email, deleted_at);

-- ============================================================
-- 第3部分：清理冗余索引
-- 这些索引的前缀已被更宽的唯一索引或复合索引覆盖
-- ============================================================

-- allowed_corp_ids: corp_id 列上有 3 个索引，只需保留 1 个 UNIQUE
ALTER TABLE allowed_corp_ids DROP INDEX corp_id_2;
ALTER TABLE allowed_corp_ids DROP INDEX idx_corp_id;

-- carriers: idx_carrier_id 被 carrier_id (UNIQUE) 覆盖
ALTER TABLE carriers DROP INDEX idx_carrier_id;

-- companies: idx_code 被 code (UNIQUE) 覆盖
ALTER TABLE companies DROP INDEX idx_code;

-- invitations: idx_token 被 token (UNIQUE) 覆盖
ALTER TABLE invitations DROP INDEX idx_token;

-- role_menus: idx_role_id 被 uk_role_menu(role_id, menu_id) 覆盖
ALTER TABLE role_menus DROP INDEX idx_role_id;

-- role_permissions: idx_role_id 被 uk_role_permission(role_id, permission_id) 覆盖
ALTER TABLE role_permissions DROP INDEX idx_role_id;

-- user_roles: idx_user_id 被 uk_user_role(user_id, role_id) 覆盖
ALTER TABLE user_roles DROP INDEX idx_user_id;

-- user_shop_roles: idx_user_id 被 uk_user_shop_role(user_id, shop_id, role_id) 覆盖
ALTER TABLE user_shop_roles DROP INDEX idx_user_id;

-- product_shops: idx_product_id 被 uk_product_shop(product_id, shop_id) 覆盖
ALTER TABLE product_shops DROP INDEX idx_product_id;

-- tracking_numbers: 3 个单列索引被复合索引覆盖
ALTER TABLE tracking_numbers DROP INDEX idx_track_status;    -- 被 idx_status_updated(track_status, updated_at) 覆盖
ALTER TABLE tracking_numbers DROP INDEX idx_carrier_code;    -- 被 idx_carrier_updated(carrier_code, updated_at) 覆盖
ALTER TABLE tracking_numbers DROP INDEX idx_user_id;         -- 被 idx_user_status_updated(user_id, track_status, updated_at) 覆盖

-- ============================================================
-- 第4部分：补充缺失索引
-- 日志表常用筛选列缺少索引
-- ============================================================

CREATE INDEX idx_username ON login_logs(username);
CREATE INDEX idx_login_result ON login_logs(login_result);
CREATE INDEX idx_username ON audit_logs(username);

-- ============================================================
-- 第5部分：审查级联删除
-- shops.fk_shops_user: 删除用户不应级联删除店铺
-- tracking_numbers.fk_tracking_user: 删除用户不应级联删除运单
-- ============================================================

-- 5.1 shops: ON DELETE CASCADE -> ON DELETE SET NULL
--     删除用户后，店铺保留，user_id 置 NULL
--     注意：user_id 原为 NOT NULL，需先改为 nullable
ALTER TABLE shops DROP FOREIGN KEY fk_shops_user;
ALTER TABLE shops MODIFY COLUMN user_id bigint NULL COMMENT '创建者用户ID';
ALTER TABLE shops ADD CONSTRAINT fk_shops_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE RESTRICT;

-- 5.2 tracking_numbers: ON DELETE CASCADE -> ON DELETE SET NULL
--     删除用户后，运单保留，user_id 置 NULL
ALTER TABLE tracking_numbers DROP FOREIGN KEY fk_tracking_user;
ALTER TABLE tracking_numbers ADD CONSTRAINT fk_tracking_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE RESTRICT;

-- ============================================================
-- 第6部分：清理测试残留表
-- ============================================================

DROP TABLE IF EXISTS demo_table;

-- ============================================================
-- 第7部分：开启慢查询日志（需要 SUPER 权限）
-- 注意：这是运行时配置，MySQL 重启后失效
-- 永久生效需写入 my.cnf:
--   [mysqld]
--   slow_query_log = ON
--   long_query_time = 1
--   slow_query_log_file = /var/log/mysql/slow.log
-- ============================================================

SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;

-- ============================================================
-- 验证脚本（执行完毕后运行以确认结果）
-- ============================================================

-- 验证1: 所有表应为 utf8mb4_unicode_ci
SELECT TABLE_NAME, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'logistics_system'
  AND TABLE_COLLATION != 'utf8mb4_unicode_ci'
  AND TABLE_NAME != 'demo_table';
-- 期望结果: 0 行

-- 验证2: users 表唯一索引已更新
SHOW INDEX FROM users WHERE Key_name LIKE 'uk_%';
-- 期望: uk_username_alive(username, deleted_at), uk_email_alive(email, deleted_at)

-- 验证3: 冗余索引已清理
SELECT TABLE_NAME, INDEX_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'logistics_system'
  AND INDEX_NAME IN ('corp_id_2', 'idx_corp_id', 'idx_carrier_id', 'idx_code',
                     'idx_token', 'idx_track_status', 'idx_carrier_code')
GROUP BY TABLE_NAME, INDEX_NAME;
-- 期望结果: 0 行

-- 验证4: 新索引已创建
SHOW INDEX FROM login_logs WHERE Key_name IN ('idx_username', 'idx_login_result');
SHOW INDEX FROM audit_logs WHERE Key_name = 'idx_username';
-- 期望: 各显示对应索引

-- 验证5: 慢查询日志已开启
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';
-- 期望: ON, 1.000000

-- 验证6: demo_table 已删除
SELECT COUNT(*) FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'logistics_system' AND TABLE_NAME = 'demo_table';
-- 期望: 0
