-- =========================================
-- 定期归档配置
-- =========================================
-- 执行时间: 2025-11-29
-- 目标: 实现自动化数据归档，保持主表性能
-- =========================================

USE logistics_system;

-- =====================================================
-- 1. 归档日志表（记录每次归档操作）
-- =====================================================
CREATE TABLE IF NOT EXISTS archive_logs (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  archive_type VARCHAR(50) NOT NULL COMMENT '归档类型',
  archived_count INT NOT NULL DEFAULT 0 COMMENT '归档数量',
  days_threshold INT NOT NULL COMMENT '归档天数阈值',
  started_at DATETIME NOT NULL COMMENT '开始时间',
  completed_at DATETIME COMMENT '完成时间',
  status VARCHAR(20) NOT NULL DEFAULT 'running' COMMENT '状态：running/success/failed',
  error_message TEXT COMMENT '错误信息',
  PRIMARY KEY (id),
  KEY idx_started_at (started_at),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='归档日志表';

-- =====================================================
-- 2. 归档存储过程（带日志记录和错误处理）
-- =====================================================
DROP PROCEDURE IF EXISTS archive_completed_trackings;

DELIMITER //

CREATE PROCEDURE archive_completed_trackings(IN days_to_keep INT)
BEGIN
    DECLARE tracking_count INT DEFAULT 0;
    DECLARE event_count INT DEFAULT 0;
    DECLARE log_id BIGINT;
    DECLARE error_msg TEXT;

    -- 异常处理：回滚并记录错误
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 error_msg = MESSAGE_TEXT;
        ROLLBACK;
        UPDATE archive_logs SET
            status = 'failed',
            error_message = error_msg,
            completed_at = NOW()
        WHERE id = log_id;
        SELECT CONCAT('归档失败: ', error_msg) AS result;
    END;

    -- 记录开始日志
    INSERT INTO archive_logs (archive_type, days_threshold, started_at, status)
    VALUES ('tracking_and_events', days_to_keep, NOW(), 'running');
    SET log_id = LAST_INSERT_ID();

    START TRANSACTION;

    -- 统计待归档数量
    SELECT COUNT(*) INTO tracking_count
    FROM tracking_numbers
    WHERE track_status = 'Delivered'
      AND delivered_time < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);

    SELECT COUNT(*) INTO event_count
    FROM tracking_events e
    INNER JOIN tracking_numbers tn ON e.tracking_id = tn.id
    WHERE tn.track_status = 'Delivered'
      AND tn.delivered_time < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);

    IF tracking_count = 0 THEN
        -- 无数据需要归档
        UPDATE archive_logs SET
            status = 'success',
            archived_count = 0,
            completed_at = NOW()
        WHERE id = log_id;
        COMMIT;
        SELECT '没有需要归档的数据' AS result, 0 AS tracking_count, 0 AS event_count;
    ELSE
        -- 1. 归档运单
        INSERT INTO tracking_numbers_history
        SELECT *, NOW() as archived_at
        FROM tracking_numbers
        WHERE track_status = 'Delivered'
          AND delivered_time < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);

        -- 2. 归档物流事件
        INSERT INTO tracking_events_history
        SELECT e.*, NOW() as archived_at
        FROM tracking_events e
        INNER JOIN tracking_numbers tn ON e.tracking_id = tn.id
        WHERE tn.track_status = 'Delivered'
          AND tn.delivered_time < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);

        -- 3. 删除主表物流事件
        DELETE e FROM tracking_events e
        INNER JOIN tracking_numbers tn ON e.tracking_id = tn.id
        WHERE tn.track_status = 'Delivered'
          AND tn.delivered_time < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);

        -- 4. 删除主表运单
        DELETE FROM tracking_numbers
        WHERE track_status = 'Delivered'
          AND delivered_time < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);

        -- 更新日志
        UPDATE archive_logs SET
            status = 'success',
            archived_count = tracking_count,
            completed_at = NOW()
        WHERE id = log_id;

        COMMIT;
        SELECT '归档成功' AS result, tracking_count, event_count;
    END IF;
END //

DELIMITER ;

-- =====================================================
-- 3. 定时归档事件（每月1号凌晨3点执行）
-- =====================================================
DROP EVENT IF EXISTS evt_monthly_archive;

CREATE EVENT evt_monthly_archive
ON SCHEDULE
    EVERY 1 MONTH
    STARTS DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-01 03:00:00')
ON COMPLETION PRESERVE
ENABLE
COMMENT '每月自动归档90天前已签收的运单和事件'
DO
    CALL archive_completed_trackings(90);

-- =====================================================
-- 验证配置
-- =====================================================
SELECT '定期归档配置完成' AS status;
SHOW EVENTS WHERE Db = 'logistics_system';

-- =====================================================
-- 使用说明
-- =====================================================
--
-- 1. 手动执行归档（归档90天前数据）:
--    CALL archive_completed_trackings(90);
--
-- 2. 查看归档日志:
--    SELECT * FROM archive_logs ORDER BY id DESC LIMIT 10;
--
-- 3. 查看定时任务:
--    SHOW EVENTS WHERE Db = 'logistics_system';
--
-- 4. 临时禁用定时归档:
--    ALTER EVENT evt_monthly_archive DISABLE;
--
-- 5. 重新启用定时归档:
--    ALTER EVENT evt_monthly_archive ENABLE;
--
-- 6. 修改归档天数（改为60天）:
--    DROP EVENT evt_monthly_archive;
--    CREATE EVENT evt_monthly_archive
--    ON SCHEDULE EVERY 1 MONTH
--    STARTS DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-01 03:00:00')
--    ON COMPLETION PRESERVE ENABLE
--    DO CALL archive_completed_trackings(60);
--
-- 7. 查看归档数据统计:
--    SELECT
--        DATE_FORMAT(archived_at, '%Y-%m') as month,
--        COUNT(*) as count
--    FROM tracking_numbers_history
--    GROUP BY DATE_FORMAT(archived_at, '%Y-%m');
-- =====================================================
