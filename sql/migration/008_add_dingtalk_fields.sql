-- 添加钉钉相关字段到users表

-- 为users表添加钉钉相关字段
ALTER TABLE users 
ADD COLUMN ding_union_id VARCHAR(100) DEFAULT NULL COMMENT '钉钉unionId（跨企业唯一标识）',
ADD COLUMN ding_user_id VARCHAR(100) DEFAULT NULL COMMENT '钉钉userId（企业内唯一标识）',
ADD COLUMN corp_id VARCHAR(100) DEFAULT NULL COMMENT '企业CorpId',
ADD COLUMN login_source VARCHAR(20) DEFAULT 'PASSWORD' COMMENT '登录来源：PASSWORD, DINGTALK';

-- 添加索引以提升查询性能
ALTER TABLE users
ADD INDEX idx_ding_union_id (ding_union_id),
ADD INDEX idx_corp_id (corp_id),
ADD INDEX idx_login_source (login_source);

-- 验证修改
DESCRIBE users;
