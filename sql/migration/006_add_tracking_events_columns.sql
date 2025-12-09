-- =========================================
-- 添加tracking_events表缺失的字段
-- =========================================
-- 执行时间: 2025-12-07
-- 目标: 修复tracking_events表缺失字段，支持Track17 V2 API完整数据存储
-- =========================================

USE logistics_system;

-- =====================================================
-- 添加Track17 V2 API所需的字段
-- =====================================================

-- 添加城市字段
ALTER TABLE tracking_events
ADD COLUMN IF NOT EXISTS city VARCHAR(100) DEFAULT NULL COMMENT '城市' AFTER event_location;

-- 添加邮编字段
ALTER TABLE tracking_events
ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20) DEFAULT NULL COMMENT '邮编' AFTER city;

-- 添加承运商ID字段
ALTER TABLE tracking_events
ADD COLUMN IF NOT EXISTS provider_key INT DEFAULT NULL COMMENT '承运商ID' AFTER postal_code;

-- 添加承运商名称字段（关键字段，用于前端分组显示）
ALTER TABLE tracking_events
ADD COLUMN IF NOT EXISTS provider_name VARCHAR(100) DEFAULT NULL COMMENT '承运商名称' AFTER provider_key;

-- 添加阶段字段
ALTER TABLE tracking_events
ADD COLUMN IF NOT EXISTS stage VARCHAR(50) DEFAULT NULL COMMENT '阶段: InfoReceived/InTransit/Delivered/Exception等' AFTER event_code;

-- 添加子状态字段
ALTER TABLE tracking_events
ADD COLUMN IF NOT EXISTS sub_status VARCHAR(100) DEFAULT NULL COMMENT '子状态' AFTER stage;

-- 添加ISO时间字符串字段
ALTER TABLE tracking_events
ADD COLUMN IF NOT EXISTS time_iso VARCHAR(50) DEFAULT NULL COMMENT 'ISO时间字符串' AFTER sub_status;

-- =====================================================
-- 添加索引（提升查询性能）
-- =====================================================

-- 承运商名称索引（用于按承运商筛选和分组）
ALTER TABLE tracking_events
ADD INDEX IF NOT EXISTS idx_provider_name (provider_name)
COMMENT '承运商名称索引-用于分组查询';

-- =====================================================
-- 验证结果
-- =====================================================
SHOW COLUMNS FROM tracking_events;

-- =====================================================
-- 说明
-- =====================================================
-- 这些字段是Track17 V2 API返回的关键数据：
-- 1. provider_name: 承运商名称，前端用于分组显示物流轨迹
-- 2. city: 事件发生城市
-- 3. postal_code: 邮编
-- 4. provider_key: 承运商在17Track系统中的ID
-- 5. stage: 物流阶段（InfoReceived/InTransit/Delivered/Exception）
-- 6. sub_status: 详细子状态（如Delivered_Other, InTransit_PickedUp）
-- 7. time_iso: ISO 8601格式的时间字符串（用于精确时间显示）
-- =====================================================
