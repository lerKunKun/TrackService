-- 给产品媒体文件表增加自定义标签字段
ALTER TABLE product_media_files
    ADD COLUMN tags VARCHAR(500) NULL COMMENT '自定义标签（逗号分隔）' AFTER category;
