-- Track17 物流追踪系统数据库
-- 使用 UTF8MB4 字符集
CREATE DATABASE IF NOT EXISTS logistics_system
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE logistics_system;

-- =====================================================
-- 删除表（按照外键依赖关系逆序删除）
-- =====================================================
DROP TABLE IF EXISTS `tracking_events`;
DROP TABLE IF EXISTS `tracking_numbers`;
DROP TABLE IF EXISTS `parcels`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `webhook_logs`;
DROP TABLE IF EXISTS `sync_jobs`;
DROP TABLE IF EXISTS `shops`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `carriers`;

-- =====================================================
-- 用户表（新增）
-- =====================================================
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `email` VARCHAR(100) COMMENT '邮箱',
  `phone` VARCHAR(20) COMMENT '手机号',
  `real_name` VARCHAR(50) COMMENT '真实姓名',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN-管理员，USER-普通用户',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `avatar` VARCHAR(255) COMMENT '头像URL',
  `last_login_time` DATETIME COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) COMMENT '最后登录IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 店铺表
-- =====================================================
CREATE TABLE `shops` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '店铺ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `shop_name` VARCHAR(100) NOT NULL COMMENT '店铺名称',
  `platform` VARCHAR(50) NOT NULL COMMENT '平台类型：shopify, shopline, tiktok',
  `shop_url` VARCHAR(255) COMMENT '店铺URL',
  `api_key` VARCHAR(255) COMMENT 'API密钥',
  `api_secret` VARCHAR(255) COMMENT 'API密钥Secret',
  `access_token` VARCHAR(500) COMMENT '访问令牌',
  `webhook_secret` VARCHAR(255) COMMENT 'Webhook密钥',
  `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否激活：0-否，1-是',
  `last_sync_time` DATETIME COMMENT '最后同步时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_platform` (`platform`),
  KEY `idx_is_active` (`is_active`),
  CONSTRAINT `fk_shops_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='店铺表';

-- =====================================================
-- 订单表
-- =====================================================
CREATE TABLE `orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `shop_id` BIGINT NOT NULL COMMENT '店铺ID',
  `order_number` VARCHAR(100) NOT NULL COMMENT '订单号',
  `platform_order_id` VARCHAR(100) COMMENT '平台订单ID',
  `customer_name` VARCHAR(100) COMMENT '客户姓名',
  `customer_email` VARCHAR(100) COMMENT '客户邮箱',
  `shipping_address` TEXT COMMENT '收货地址（JSON格式）',
  `order_status` VARCHAR(50) COMMENT '订单状态',
  `total_amount` DECIMAL(10,2) COMMENT '订单金额',
  `currency` VARCHAR(10) COMMENT '币种',
  `order_date` DATETIME COMMENT '订单日期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_number` (`order_number`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_order_date` (`order_date`),
  CONSTRAINT `fk_orders_shop` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- =====================================================
-- 包裹表
-- =====================================================
CREATE TABLE `parcels` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '包裹ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `parcel_number` VARCHAR(100) COMMENT '包裹号',
  `weight` DECIMAL(8,2) COMMENT '重量(kg)',
  `length` DECIMAL(8,2) COMMENT '长度(cm)',
  `width` DECIMAL(8,2) COMMENT '宽度(cm)',
  `height` DECIMAL(8,2) COMMENT '高度(cm)',
  `parcel_status` VARCHAR(50) COMMENT '包裹状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  CONSTRAINT `fk_parcels_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='包裹表';

-- =====================================================
-- 运单表
-- =====================================================
CREATE TABLE `tracking_numbers` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '运单ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `parcel_id` BIGINT COMMENT '包裹ID（可为空，手动添加的运单）',
  `tracking_number` VARCHAR(100) NOT NULL COMMENT '运单号',
  `carrier_code` VARCHAR(50) COMMENT '承运商代码',
  `carrier_name` VARCHAR(100) COMMENT '承运商名称',
  `destination_country` VARCHAR(50) COMMENT '目的地国家',
  `package_status` VARCHAR(50) COMMENT '包裹状态',
  `track17_status` VARCHAR(50) COMMENT '17Track状态码',
  `track17_substatus` VARCHAR(50) COMMENT '17Track子状态码',
  `latest_event` TEXT COMMENT '最新物流事件',
  `registered_at_17track` TINYINT DEFAULT 0 COMMENT '是否已注册到17Track：0-否，1-是',
  `last_sync_time` DATETIME COMMENT '最后同步时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tracking_number` (`tracking_number`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parcel_id` (`parcel_id`),
  KEY `idx_carrier_code` (`carrier_code`),
  KEY `idx_package_status` (`package_status`),
  CONSTRAINT `fk_tracking_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tracking_parcel` FOREIGN KEY (`parcel_id`) REFERENCES `parcels` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运单表';

-- =====================================================
-- 物流事件表
-- =====================================================
CREATE TABLE `tracking_events` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '事件ID',
  `tracking_id` BIGINT NOT NULL COMMENT '运单ID',
  `event_time` DATETIME NOT NULL COMMENT '事件时间',
  `event_description` TEXT COMMENT '事件描述',
  `event_location` VARCHAR(255) COMMENT '事件地点',
  `event_code` VARCHAR(50) COMMENT '事件代码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tracking_id` (`tracking_id`),
  KEY `idx_event_time` (`event_time`),
  CONSTRAINT `fk_events_tracking` FOREIGN KEY (`tracking_id`) REFERENCES `tracking_numbers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流事件表';

-- =====================================================
-- 承运商表
-- =====================================================
CREATE TABLE `carriers` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '承运商ID',
  `carrier_code` VARCHAR(50) NOT NULL COMMENT '承运商代码',
  `carrier_name` VARCHAR(100) NOT NULL COMMENT '承运商名称',
  `carrier_name_en` VARCHAR(100) COMMENT '承运商英文名称',
  `carrier_url` VARCHAR(255) COMMENT '承运商官网',
  `track17_code` VARCHAR(50) COMMENT '17Track承运商代码',
  `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-否，1-是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_carrier_code` (`carrier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='承运商表';

-- =====================================================
-- Webhook日志表
-- =====================================================
CREATE TABLE `webhook_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `shop_id` BIGINT COMMENT '店铺ID',
  `webhook_type` VARCHAR(50) COMMENT 'Webhook类型',
  `payload` TEXT COMMENT '请求体（JSON）',
  `headers` TEXT COMMENT '请求头（JSON）',
  `status` VARCHAR(20) COMMENT '处理状态：success, failed',
  `error_message` TEXT COMMENT '错误信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Webhook日志表';

-- =====================================================
-- 同步任务表
-- =====================================================
CREATE TABLE `sync_jobs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_type` VARCHAR(50) NOT NULL COMMENT '任务类型：order_sync, tracking_sync',
  `shop_id` BIGINT COMMENT '店铺ID',
  `status` VARCHAR(20) NOT NULL COMMENT '任务状态：pending, running, completed, failed',
  `total_count` INT DEFAULT 0 COMMENT '总数',
  `success_count` INT DEFAULT 0 COMMENT '成功数',
  `failed_count` INT DEFAULT 0 COMMENT '失败数',
  `error_message` TEXT COMMENT '错误信息',
  `started_at` DATETIME COMMENT '开始时间',
  `completed_at` DATETIME COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同步任务表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 插入默认管理员账号
-- 用户名: admin
-- 密码: admin123 (BCrypt加密后的值)
-- BCrypt加密的 "admin123": $2a$10$XkKmz8LqVvQz9J0x0YxW3.KqZ7qYQYYqYJx8x8x8x8x8x8x8x8x8x
-- 注意：下面的密码hash需要在应用启动后通过程序生成真实的BCrypt值
INSERT INTO `users` (`username`, `password`, `email`, `real_name`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2B.0A6VhNcVzLv0GNTD8VQ2', 'admin@track17.com', '系统管理员', 'ADMIN', 1);

-- 插入常用承运商数据
INSERT INTO `carriers` (`carrier_code`, `carrier_name`, `carrier_name_en`, `carrier_url`, `track17_code`) VALUES
('ups', 'UPS', 'United Parcel Service', 'https://www.ups.com', '1'),
('usps', 'USPS', 'United States Postal Service', 'https://www.usps.com', '2'),
('fedex', 'FedEx', 'Federal Express', 'https://www.fedex.com', '3'),
('dhl', 'DHL', 'DHL Express', 'https://www.dhl.com', '4'),
('chinapost', '中国邮政', 'China Post', 'http://www.chinapost.com.cn', '5'),
('ems', 'EMS', 'Express Mail Service', 'http://www.ems.com.cn', '6'),
('sf-express', '顺丰速运', 'SF Express', 'https://www.sf-express.com', '7'),
('yanwen', '燕文物流', 'Yanwen Logistics', 'https://www.yw56.com.cn', '8'),
('4px', '递四方', '4PX Express', 'https://www.4px.com', '9'),
('yuntu', '云途物流', 'Yun Express', 'https://www.yunexpress.com', '10');
