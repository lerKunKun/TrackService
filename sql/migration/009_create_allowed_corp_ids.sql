-- 创建允许登录的企业CorpId白名单表
CREATE TABLE allowed_corp_ids (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    corp_id VARCHAR(100) NOT NULL UNIQUE COMMENT '允许的企业CorpId',
    corp_name VARCHAR(200) COMMENT '企业名称',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_corp_id (corp_id),
    INDEX idx_status (status)
) COMMENT='允许登录的企业CorpId白名单';

-- 插入当前配置的CorpId作为初始数据
INSERT INTO allowed_corp_ids (corp_id, corp_name, created_by) 
VALUES ('ding5a2a2426f8bfaff7a1320dcb25e91351', '默认企业', 'SYSTEM');

-- 验证创建
SELECT * FROM allowed_corp_ids;
