-- =====================================================
-- Phase 1: Company + Multi-Tenant Data Model Migration
-- =====================================================
-- 运行前请备份数据库！
-- 执行方式: docker exec -i <container> mysql -uroot -p<password> <database> < migration_v2_company.sql

-- 1. 创建公司表
CREATE TABLE IF NOT EXISTS companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '公司名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '公司编码（唯一标识）',
    owner_user_id BIGINT COMMENT '创建者/所有者 user_id',
    logo VARCHAR(500) COMMENT '公司 Logo URL',
    status TINYINT DEFAULT 1 COMMENT '0-禁用, 1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_owner (owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司/租户表';

-- 2. 创建公司成员表
CREATE TABLE IF NOT EXISTS company_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL COMMENT '公司 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    role_id BIGINT COMMENT '公司级角色 ID',
    invited_by BIGINT COMMENT '邀请人 user_id',
    joined_at DATETIME COMMENT '加入时间',
    status TINYINT DEFAULT 1 COMMENT '0-待确认, 1-已加入, 2-已离开',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_company_user (company_id, user_id),
    INDEX idx_user (user_id),
    INDEX idx_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司成员表';

-- 3. 创建邀请表
CREATE TABLE IF NOT EXISTS invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL COMMENT '公司 ID',
    shop_id BIGINT COMMENT '店铺 ID（可选）',
    email VARCHAR(255) NOT NULL COMMENT '被邀请人邮箱',
    role_id BIGINT COMMENT '分配的角色 ID',
    token VARCHAR(100) NOT NULL UNIQUE COMMENT '邀请 Token',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/EXPIRED/REVOKED',
    expires_at DATETIME COMMENT '过期时间',
    invited_by BIGINT COMMENT '邀请人 user_id',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_token (token),
    INDEX idx_email (email),
    INDEX idx_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请表';

-- 4. 给 shops 表添加 company_id 字段
ALTER TABLE shops ADD COLUMN company_id BIGINT COMMENT '所属公司 ID' AFTER id;
ALTER TABLE shops ADD INDEX idx_company_id (company_id);

-- 5. 数据迁移：为现有数据创建默认公司
-- 注意：需要根据实际情况调整，以下是示例

-- 5.1 如果还没有默认公司，创建一个
INSERT INTO companies (name, code, owner_user_id, status)
SELECT '默认公司', 'DEFAULT', MIN(id), 1
FROM users
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE code = 'DEFAULT');

-- 5.2 将所有现有店铺关联到默认公司
UPDATE shops SET company_id = (SELECT id FROM companies WHERE code = 'DEFAULT')
WHERE company_id IS NULL;

-- 5.3 将 user_roles 表中的现有用户迁移为默认公司的 company_members
INSERT IGNORE INTO company_members (company_id, user_id, role_id, joined_at, status)
SELECT
    (SELECT id FROM companies WHERE code = 'DEFAULT'),
    ur.user_id,
    ur.role_id,
    NOW(),
    1
FROM user_roles ur;

-- 6. 删除不再使用的 product_visibility 表（如果存在）
DROP TABLE IF EXISTS product_visibility;

SELECT '迁移完成！' AS result;
