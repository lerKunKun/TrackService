-- V14: 邮箱监控支持 Microsoft OAuth2 Modern Authentication
ALTER TABLE email_monitor_config
    MODIFY COLUMN password VARCHAR(500) NULL COMMENT '密码（PASSWORD 认证时使用，OAUTH2 时为空）',
    ADD COLUMN auth_type VARCHAR(20) NOT NULL DEFAULT 'PASSWORD' COMMENT '认证类型: PASSWORD / OAUTH2_MICROSOFT',
    ADD COLUMN access_token TEXT COMMENT 'OAuth2 访问令牌（1小时有效）',
    ADD COLUMN refresh_token TEXT COMMENT 'OAuth2 刷新令牌（长期有效）',
    ADD COLUMN token_expiry DATETIME COMMENT 'access_token 过期时间';
