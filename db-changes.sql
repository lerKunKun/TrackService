-- =============================================
-- product_media 表
-- =============================================
CREATE TABLE IF NOT EXISTS `product_media` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `product_id` BIGINT NOT NULL COMMENT '产品ID',
    `reference_link` json DEFAULT NULL COMMENT '对标页链接列表(JSON数组)',
    `main_images` json DEFAULT NULL COMMENT '主图URL列表',
    `detail_media` json DEFAULT NULL COMMENT '详情页媒体文件URL列表',
    `ad_media` json DEFAULT NULL COMMENT '广告媒体素材URL列表',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_id` (`product_id`),
    CONSTRAINT `fk_product_media_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品媒体表';

-- =============================================
-- product_template 表
-- =============================================
CREATE TABLE IF NOT EXISTS `product_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `product_id` BIGINT NOT NULL COMMENT '产品ID',
    `template_name` varchar(255) DEFAULT NULL COMMENT '模板名称',
    `template_version` varchar(255) DEFAULT NULL COMMENT '模板版本',
    `store_identifier` varchar(255) DEFAULT NULL COMMENT '店铺标识符',
    `product_json_content` json DEFAULT NULL COMMENT '产品json内容',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_id` (`product_id`),
    KEY `idx_store_identifier` (`store_identifier`),
    CONSTRAINT `fk_product_template_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品模板表';

-- =============================================
-- 已有表的修复（增量ALTER，幂等执行）
-- =============================================

-- 修复 product_media.reference_link 类型: text -> json
-- ALTER TABLE `product_media` MODIFY COLUMN `reference_link` json DEFAULT NULL COMMENT '对标页链接列表(JSON数组)';

-- 补充 product_template.store_identifier 索引（如表已存在但缺少索引）
-- ALTER TABLE `product_template` ADD INDEX `idx_store_identifier` (`store_identifier`);

-- 补充外键（如表已存在但缺少外键）
-- ALTER TABLE `product_media` ADD CONSTRAINT `fk_product_media_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `product_template` ADD CONSTRAINT `fk_product_template_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE;

-- =============================================
-- 产品媒体文件表（DB+MinIO 双写架构）
-- =============================================
CREATE TABLE IF NOT EXISTS `product_media_files` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `product_id` BIGINT NOT NULL,
    `category` VARCHAR(30) NOT NULL COMMENT 'main_image|detail_media|ad_media|document',
    `original_name` VARCHAR(500) NOT NULL COMMENT '原始文件名',
    `object_name` VARCHAR(500) NOT NULL COMMENT 'MinIO 对象路径',
    `content_type` VARCHAR(100) COMMENT 'MIME 类型',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(bytes)',
    `media_type` VARCHAR(20) NOT NULL COMMENT 'image|video|document',
    `url` VARCHAR(1000) NOT NULL COMMENT '直接访问 URL',
    `source` VARCHAR(30) DEFAULT 'UPLOAD' COMMENT 'UPLOAD|URL_DOWNLOAD|SHOPIFY_SYNC|SUPPLIER',
    `source_url` VARCHAR(2000) COMMENT '原始来源 URL',
    `sort_order` INT DEFAULT 0,
    `uploader_id` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_object_name` (`object_name`),
    INDEX `idx_product_category` (`product_id`, `category`),
    INDEX `idx_product_sort` (`product_id`, `sort_order`),
    FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品媒体文件';

-- =============================================
-- 产品模板预览功能 - 增量变更
-- =============================================

-- shops 表添加开发者店铺标记
ALTER TABLE shops ADD COLUMN IF NOT EXISTS is_dev_store TINYINT DEFAULT 0
  COMMENT '是否为开发者店铺(预览推送目标)';

-- product_template 表添加主题文件缓存及推送跟踪字段
ALTER TABLE product_template
  ADD COLUMN IF NOT EXISTS source_shop_id BIGINT COMMENT '源店铺ID(拉取主题文件)',
  ADD COLUMN IF NOT EXISTS theme_settings JSON COMMENT 'settings_data.json 缓存',
  ADD COLUMN IF NOT EXISTS footer_group JSON COMMENT 'footer-group.json 缓存',
  ADD COLUMN IF NOT EXISTS last_pull_time DATETIME COMMENT '最后拉取时间',
  ADD COLUMN IF NOT EXISTS last_push_time DATETIME COMMENT '最后推送时间',
  ADD COLUMN IF NOT EXISTS dev_product_id BIGINT COMMENT '在开发者店铺中创建的产品ID';
-- ALTER TABLE product_template ADD INDEX idx_source_shop (source_shop_id);
