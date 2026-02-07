-- Add publish_status and last_export_time to product_shop table
-- For Product Listing MVP (CSV Export)

ALTER TABLE `product_shops` 
ADD COLUMN `publish_status` TINYINT DEFAULT 0 COMMENT '刊登状态: 0-未刊登, 1-已导出(CSV), 2-已发布(API成功), 3-失败',
ADD COLUMN `last_export_time` DATETIME COMMENT '最后导出CSV时间';
