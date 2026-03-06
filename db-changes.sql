CREATE TABLE IF NOT EXISTS `product_media` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `product_id` bigint(20) NOT NULL COMMENT '产品ID',
    `reference_link` varchar(1024) DEFAULT NULL COMMENT '对标页链接',
    `main_images` json DEFAULT NULL COMMENT '主图URL列表',
    `detail_media` json DEFAULT NULL COMMENT '详情页媒体文件URL列表',
    `ad_media` json DEFAULT NULL COMMENT '广告媒体素材URL列表',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品媒体表';

CREATE TABLE IF NOT EXISTS `product_template` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `product_id` bigint(20) NOT NULL COMMENT '产品ID',
    `template_name` varchar(255) DEFAULT NULL COMMENT '模板名称',
    `template_version` varchar(255) DEFAULT NULL COMMENT '模板版本',
    `store_identifier` varchar(255) DEFAULT NULL COMMENT '店铺标识符',
    `product_json_content` json DEFAULT NULL COMMENT '产品json内容',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品模板表';
