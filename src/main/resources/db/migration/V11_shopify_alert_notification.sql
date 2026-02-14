-- V11: Shopify Alert Notification tables
-- 通知推送日志
CREATE TABLE IF NOT EXISTS notification_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shop_id BIGINT COMMENT '店铺ID(可NULL)',
    alert_type VARCHAR(50) NOT NULL COMMENT '类型: DISPUTE/INFRINGEMENT/ACCOUNT/APP_UNINSTALLED/SHOP_ALERT/DAILY_SUMMARY/MONTHLY_SUMMARY',
    severity VARCHAR(20) COMMENT '严重度: CRITICAL/HIGH/MEDIUM/INFO',
    source VARCHAR(20) COMMENT '数据源: WEBHOOK/API_POLL/EMAIL',
    title VARCHAR(200) COMMENT '消息标题',
    content TEXT COMMENT '消息内容',
    dedup_key VARCHAR(200) COMMENT '去重键',
    recipient_userid VARCHAR(100) COMMENT '接收人钉钉userid',
    send_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
    error_message VARCHAR(500),
    sent_time DATETIME,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dedup (dedup_key)
) COMMENT '通知推送日志';

-- 邮箱监控配置(前端页面动态管理，支持多邮箱)
CREATE TABLE IF NOT EXISTS email_monitor_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '配置名称(如: Shopify主邮箱)',
    host VARCHAR(200) NOT NULL COMMENT 'IMAP服务器地址',
    port INT DEFAULT 993 COMMENT '端口',
    protocol VARCHAR(10) DEFAULT 'imaps' COMMENT '协议: imaps/imap',
    username VARCHAR(200) NOT NULL COMMENT '邮箱账号',
    password VARCHAR(500) NOT NULL COMMENT '密码(加密存储)',
    sender_filter VARCHAR(200) DEFAULT 'noreply@shopify.com' COMMENT '发件人过滤',
    check_interval INT DEFAULT 300 COMMENT '检查间隔(秒)',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    last_check_time DATETIME COMMENT '上次检查时间',
    last_check_status VARCHAR(20) COMMENT 'SUCCESS/FAILED',
    last_error_message VARCHAR(500),
    created_by VARCHAR(50),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '邮箱监控配置';

-- 通知接收人(前端页面动态管理，支持多人)
CREATE TABLE IF NOT EXISTS notification_recipient (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    dingtalk_userid VARCHAR(100) NOT NULL COMMENT '钉钉userId',
    alert_types VARCHAR(500) DEFAULT 'ALL' COMMENT '订阅的Alert类型(逗号分隔或ALL)',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(200) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_userid (dingtalk_userid)
) COMMENT '通知接收人配置';
