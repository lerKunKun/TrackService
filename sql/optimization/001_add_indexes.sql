-- =========================================
-- 运单管理系统 - 索引优化脚本
-- =========================================
-- 执行时间: 2025-11-23
-- 目标: 优化查询性能，支持100万+运单
-- 预计执行时间: 5-10分钟（取决于数据量）
-- =========================================

USE logistics_system;

-- 1. 状态索引（高频查询字段）
-- 用途: WHERE track_status = 'xxx'
ALTER TABLE tracking_numbers
ADD INDEX idx_track_status (track_status)
COMMENT '状态索引-用于状态筛选';

-- 2. 创建时间索引（日期范围查询）
-- 用途: WHERE created_at >= 'xxx' AND created_at <= 'xxx'
ALTER TABLE tracking_numbers
ADD INDEX idx_created_at (created_at)
COMMENT '创建时间索引-用于日期范围查询';

-- 3. 更新时间索引（ORDER BY 排序）
-- 用途: ORDER BY updated_at DESC
ALTER TABLE tracking_numbers
ADD INDEX idx_updated_at (updated_at DESC)
COMMENT '更新时间索引-用于排序';

-- 4. 复合索引：状态+更新时间（覆盖常见查询）
-- 用途: WHERE track_status = 'xxx' ORDER BY updated_at DESC
ALTER TABLE tracking_numbers
ADD INDEX idx_status_updated (track_status, updated_at DESC)
COMMENT '状态+时间复合索引-覆盖状态筛选+排序';

-- 5. 复合索引：承运商+更新时间
-- 用途: WHERE carrier_code = 'xxx' ORDER BY updated_at DESC
ALTER TABLE tracking_numbers
ADD INDEX idx_carrier_updated (carrier_code, updated_at DESC)
COMMENT '承运商+时间复合索引-覆盖承运商筛选+排序';

-- 6. 复合索引：用户+状态+时间（多租户场景）
-- 用途: WHERE user_id = xxx AND track_status = 'xxx' ORDER BY updated_at DESC
ALTER TABLE tracking_numbers
ADD INDEX idx_user_status_updated (user_id, track_status, updated_at DESC)
COMMENT '用户+状态+时间复合索引-多租户查询优化';

-- 7. 下次同步时间索引（定时任务专用）
-- 用途: WHERE next_sync_at <= NOW() AND next_sync_at IS NOT NULL
ALTER TABLE tracking_numbers
ADD INDEX idx_next_sync (next_sync_at)
COMMENT '下次同步时间索引-定时任务查询优化';

-- 8. 来源字段索引（可能的筛选条件）
-- 用途: WHERE source = 'xxx'
ALTER TABLE tracking_numbers
ADD INDEX idx_source (source)
COMMENT '来源索引-用于来源筛选';

-- =========================================
-- 验证索引创建
-- =========================================
SELECT
    TABLE_NAME,
    INDEX_NAME,
    SEQ_IN_INDEX,
    COLUMN_NAME,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'logistics_system'
  AND TABLE_NAME = 'tracking_numbers'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- =========================================
-- 查看索引大小
-- =========================================
SELECT
    TABLE_NAME,
    ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2) AS 'Total Size (MB)',
    ROUND((INDEX_LENGTH / 1024 / 1024), 2) AS 'Index Size (MB)'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'logistics_system'
  AND TABLE_NAME = 'tracking_numbers';

-- =========================================
-- 完成
-- =========================================
-- 索引创建完成！
--
-- 下一步建议:
-- 1. 运行 ANALYZE TABLE tracking_numbers; 更新统计信息
-- 2. 使用 EXPLAIN 检查查询执行计划
-- 3. 监控慢查询日志，验证优化效果
-- =========================================
