-- ===================================================
-- V1.5.0: 产品管理模块 - 数据库表结构
-- 基于 Shopify 产品模型，支持多商店关联
-- ===================================================

-- 1. 产品主表 (基于 Shopify Product)
CREATE TABLE IF NOT EXISTS `products` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '产品ID',
  `handle` VARCHAR(255) NOT NULL COMMENT '产品唯一标识符(用于URL)',
  `title` VARCHAR(255) NOT NULL COMMENT '产品标题',
  `body_html` TEXT COMMENT '产品描述(HTML格式)',
  `vendor` VARCHAR(255) COMMENT '品牌/制造商',
  `tags` VARCHAR(500) COMMENT '标签(逗号分隔)',
  `published` TINYINT NOT NULL DEFAULT 0 COMMENT '上架状态: 0-草稿, 1-已上架',
  `product_url` VARCHAR(500) COMMENT '产品刊登链接(P3使用)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_handle` (`handle`),
  KEY `idx_title` (`title`),
  KEY `idx_published` (`published`),
  KEY `idx_vendor` (`vendor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品主表';

-- 2. 产品商店关联表 (多对多关系)
CREATE TABLE IF NOT EXISTS `product_shops` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `product_id` BIGINT NOT NULL COMMENT '产品ID',
  `shop_id` BIGINT NOT NULL COMMENT '商店ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_shop` (`product_id`, `shop_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_shop_id` (`shop_id`),
  CONSTRAINT `fk_product_shops_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_product_shops_shop` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品商店关联表';

-- 3. 产品变体表 (基于 Shopify Variant)
CREATE TABLE IF NOT EXISTS `product_variants` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '变体ID',
  `product_id` BIGINT NOT NULL COMMENT '产品ID',
  `title` VARCHAR(255) COMMENT '变体标题',
  `price` DECIMAL(10, 2) NOT NULL COMMENT '销售价格',
  `compare_at_price` DECIMAL(10, 2) COMMENT '原价(对比价格)',
  `image_url` VARCHAR(500) COMMENT '变体图片URL',
  `inventory_quantity` INT DEFAULT 0 COMMENT '库存数量',
  `weight` DECIMAL(10, 2) COMMENT '重量(克)',
  `barcode` VARCHAR(100) COMMENT '条形码',
  -- P2 采购扩展字段
  `option1_name` VARCHAR(100) COMMENT '选项1名称(如: Color)',
  `option1_value` VARCHAR(100) COMMENT '选项1值(如: Red)',
  `option2_name` VARCHAR(100) COMMENT '选项2名称(如: Size)',
  `option2_value` VARCHAR(100) COMMENT '选项2值(如: Large)',
  `option3_name` VARCHAR(100) COMMENT '选项3名称(如: Material)',
  `option3_value` VARCHAR(100) COMMENT '选项3值(如: Cotton)',
  `sku` VARCHAR(100) COMMENT 'SKU库存单位',
  `procurement_url` VARCHAR(500) COMMENT '采购商品链接',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sku` (`sku`),
  CONSTRAINT `fk_product_variants_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品变体表';

-- 4. 产品图片表 (可选，MVP阶段简化到variant.image_url)
CREATE TABLE IF NOT EXISTS `product_images` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `product_id` BIGINT NOT NULL COMMENT '产品ID',
  `variant_id` BIGINT COMMENT '关联的变体ID(可选)',
  `src` VARCHAR(500) NOT NULL COMMENT '图片URL',
  `alt_text` VARCHAR(255) COMMENT '替代文本',
  `position` INT NOT NULL DEFAULT 1 COMMENT '排序位置',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_variant_id` (`variant_id`),
  KEY `idx_position` (`position`),
  CONSTRAINT `fk_product_images_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_product_images_variant` FOREIGN KEY (`variant_id`) REFERENCES `product_variants` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品图片表';

-- 5. 产品导入记录表
CREATE TABLE IF NOT EXISTS `product_imports` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '导入记录ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '导入文件名',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '导入状态: 0-进行中, 1-成功, 2-失败',
  `total_records` INT DEFAULT 0 COMMENT '总记录数',
  `success_records` INT DEFAULT 0 COMMENT '成功导入记录数',
  `error_message` TEXT COMMENT '错误信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `completed_at` DATETIME COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品导入记录表';

-- 6. SKU申请记录表 (P2使用)
CREATE TABLE IF NOT EXISTS `sku_applications` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `product_variant_id` BIGINT NOT NULL COMMENT '产品变体ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '申请状态: 0-待审核, 1-已通过, 2-已拒绝',
  `generated_sku` VARCHAR(100) COMMENT '生成的SKU',
  `applied_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `reviewed_at` DATETIME COMMENT '审核时间',
  PRIMARY KEY (`id`),
  KEY `idx_variant_id` (`product_variant_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_sku_applications_variant` FOREIGN KEY (`product_variant_id`) REFERENCES `product_variants` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SKU申请记录表';
