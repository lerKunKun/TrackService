-- 登录日志表
CREATE TABLE IF NOT EXISTS login_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(100),
    login_type VARCHAR(20) COMMENT 'PASSWORD/DINGTALK/LOGOUT',
    login_result VARCHAR(20) COMMENT 'SUCCESS/FAILURE',
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    device VARCHAR(200),
    browser VARCHAR(100),
    os VARCHAR(100),
    error_msg VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_login_type (login_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';
