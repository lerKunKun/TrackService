-- Shopify商店信息功能拓展 - 数据库迁移脚本
-- 创建日期: 2025-12-11
-- 描述: 添加新字段用于存储更丰富的Shopify商店信息

-- 添加商店详细信息字段
ALTER TABLE shops
ADD COLUMN contact_email VARCHAR(255) COMMENT '商店联系邮箱',
ADD COLUMN owner_email VARCHAR(255) COMMENT '店主邮箱',
ADD COLUMN currency VARCHAR(10) COMMENT '商店货币代码',
ADD COLUMN plan_name VARCHAR(100) COMMENT '订阅计划名称',
ADD COLUMN plan_display_name VARCHAR(100) COMMENT '计划显示名称',
ADD COLUMN is_shopify_plus BOOLEAN DEFAULT FALSE COMMENT '是否为Shopify Plus',
ADD COLUMN primary_domain VARCHAR(255) COMMENT '主域名',
ADD COLUMN shop_owner VARCHAR(255) COMMENT '店主姓名',
ADD COLUMN iana_timezone VARCHAR(100) COMMENT 'IANA时区标识',
ADD COLUMN shop_info_json TEXT COMMENT '完整商店信息JSON（用于扩展存储）';

-- 为常用查询字段添加索引
CREATE INDEX idx_shops_currency ON shops(currency);
CREATE INDEX idx_shops_plan_name ON shops(plan_name);

-- 验证字段已添加
SELECT 
    COLUMN_NAME, 
    COLUMN_TYPE, 
    IS_NULLABLE, 
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'logistics_system'
AND TABLE_NAME = 'shops'
AND COLUMN_NAME IN (
    'contact_email', 
    'owner_email', 
    'currency', 
    'plan_name', 
    'plan_display_name', 
    'is_shopify_plus', 
    'primary_domain', 
    'shop_owner', 
    'iana_timezone', 
    'shop_info_json'
);
