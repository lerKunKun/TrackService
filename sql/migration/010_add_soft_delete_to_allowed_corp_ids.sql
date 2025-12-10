-- 为allowed_corp_ids表添加软删除字段
ALTER TABLE allowed_corp_ids 
ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间戳';

-- 添加索引
ALTER TABLE allowed_corp_ids
ADD INDEX idx_deleted_at (deleted_at);
