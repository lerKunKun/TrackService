-- =========================================
-- 运单管理系统 - 数据归档表
-- =========================================
-- 执行时间: 2025-11-23
-- 目标: 创建历史表，实现冷热数据分离
-- =========================================

USE logistics_system;

-- 1. 创建历史运单表（与主表结构相同）
CREATE TABLE IF NOT EXISTS tracking_numbers_history (
  `id` bigint NOT NULL COMMENT '运单ID',
  `user_id` bigint DEFAULT NULL,
  `parcel_id` bigint DEFAULT NULL COMMENT '包裹ID',
  `tracking_number` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '运单号',
  `carrier_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '承运商代码',
  `carrier_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '承运商名称',
  `carrier_id` int DEFAULT NULL COMMENT '17Track承运商ID',
  `source` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remarks` text COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `track_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sub_status` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子状态',
  `sub_status_descr` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子状态描述',
  `days_of_transit` int DEFAULT NULL COMMENT '运输天数',
  `days_after_last_update` int DEFAULT NULL COMMENT '距离最后更新天数',
  `latest_event_time` datetime DEFAULT NULL COMMENT '最新事件时间',
  `latest_event_desc` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最新事件描述',
  `latest_event_location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最新事件地点',
  `pickup_time` datetime DEFAULT NULL COMMENT '揽收时间',
  `delivered_time` datetime DEFAULT NULL COMMENT '签收时间',
  `last_sync_at` datetime DEFAULT NULL,
  `next_sync_at` datetime DEFAULT NULL,
  `raw_status` text COLLATE utf8mb4_unicode_ci,
  `destination_country` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目的地国家',
  `origin_country` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '始发国家代码',
  `package_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '包裹状态',
  `track17_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '17Track状态码',
  `track17_substatus` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '17Track子状态码',
  `latest_event` text COLLATE utf8mb4_unicode_ci COMMENT '最新物流事件',
  `registered_at_17track` tinyint DEFAULT '0' COMMENT '是否已注册到17Track',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最后同步时间',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
  `archived_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  PRIMARY KEY (`id`),
  KEY `idx_tracking_number` (`tracking_number`),
  KEY `idx_delivered_time` (`delivered_time`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='历史运单表';

-- 2. 创建历史事件表
CREATE TABLE IF NOT EXISTS tracking_events_history (
  `id` bigint NOT NULL COMMENT '事件ID',
  `tracking_id` bigint NOT NULL COMMENT '运单ID',
  `event_time` datetime NOT NULL COMMENT '事件时间',
  `event_description` text COLLATE utf8mb4_unicode_ci COMMENT '事件描述',
  `event_location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '事件地点',
  `city` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '城市',
  `postal_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮编',
  `provider_key` int DEFAULT NULL COMMENT '承运商ID',
  `provider_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '承运商名称',
  `event_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '事件代码',
  `stage` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '阶段',
  `sub_status` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子状态',
  `time_iso` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ISO时间字符串',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `archived_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  PRIMARY KEY (`id`),
  KEY `idx_tracking_id` (`tracking_id`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='历史物流事件表';

-- =========================================
-- 归档存储过程
-- =========================================
DELIMITER //

CREATE PROCEDURE archive_completed_trackings(IN days_to_keep INT)
BEGIN
    DECLARE archived_count INT DEFAULT 0;

    -- 开启事务
    START TRANSACTION;

    -- 1. 归档运单（已完成且超过指定天数）
    INSERT INTO tracking_numbers_history
    SELECT *, NOW() as archived_at
    FROM tracking_numbers
    WHERE track_status = 'Delivered'
      AND delivered_time < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);

    SET archived_count = ROW_COUNT();

    -- 2. 归档对应的物流事件
    INSERT INTO tracking_events_history
    SELECT e.*, NOW() as archived_at
    FROM tracking_events e
    INNER JOIN tracking_numbers tn ON e.tracking_id = tn.id
    WHERE tn.track_status = 'Delivered'
      AND tn.delivered_time < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);

    -- 3. 删除主表中的物流事件
    DELETE e FROM tracking_events e
    INNER JOIN tracking_numbers tn ON e.tracking_id = tn.id
    WHERE tn.track_status = 'Delivered'
      AND tn.delivered_time < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);

    -- 4. 删除主表中的运单
    DELETE FROM tracking_numbers
    WHERE track_status = 'Delivered'
      AND delivered_time < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);

    -- 提交事务
    COMMIT;

    -- 输出归档数量
    SELECT archived_count AS '归档运单数量';
END //

DELIMITER ;

-- =========================================
-- 使用示例
-- =========================================
-- 归档90天前已完成的运单:
-- CALL archive_completed_trackings(90);
--
-- 查看归档数据:
-- SELECT COUNT(*) FROM tracking_numbers_history;
--
-- 建议:
-- 1. 设置定时任务，每月执行一次归档
-- 2. 归档前做好备份
-- 3. 根据业务需求调整保留天数（30/60/90天）
-- =========================================

-- 验证表创建
SHOW TABLES LIKE '%history%';
