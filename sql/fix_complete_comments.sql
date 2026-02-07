-- ========== 完整修复所有表和字段注释 ==========
-- 使用UTF-8编码执行

-- ========== 表注释修复 ==========

ALTER TABLE fulfillments COMMENT '订单履约表';
ALTER TABLE products COMMENT '产品主表';

-- ========== 字段注释修复 ==========

-- products 表 - 完整字段注释
ALTER TABLE products MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '产品ID';
ALTER TABLE products MODIFY COLUMN handle VARCHAR(255) NOT NULL COMMENT '产品唯一标识符(用于URL)';
ALTER TABLE products MODIFY COLUMN title VARCHAR(500) NOT NULL COMMENT '产品标题';
ALTER TABLE products MODIFY COLUMN body_html TEXT COMMENT '产品描述(HTML格式)';
ALTER TABLE products MODIFY COLUMN vendor VARCHAR(255) COMMENT '品牌/制造商';
ALTER TABLE products MODIFY COLUMN tags VARCHAR(1000) COMMENT '标签(逗号分隔)';
ALTER TABLE products MODIFY COLUMN published TINYINT DEFAULT 0 COMMENT '上架状态 0-草稿 1-已上架';
ALTER TABLE products MODIFY COLUMN product_url VARCHAR(500) COMMENT '产品链接';
ALTER TABLE products MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE products MODIFY COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- product_variants 表 - 完整字段注释
ALTER TABLE product_variants MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '变体ID';
ALTER TABLE product_variants MODIFY COLUMN product_id BIGINT NOT NULL COMMENT '产品ID';
ALTER TABLE product_variants MODIFY COLUMN title VARCHAR(255) COMMENT '变体标题';
ALTER TABLE product_variants MODIFY COLUMN price DECIMAL(10,2) COMMENT '价格';
ALTER TABLE product_variants MODIFY COLUMN compare_at_price DECIMAL(10,2) COMMENT '原价(对比价格)';
ALTER TABLE product_variants MODIFY COLUMN image_url VARCHAR(500) COMMENT '变体图片URL';
ALTER TABLE product_variants MODIFY COLUMN inventory_quantity INT COMMENT '库存数量';
ALTER TABLE product_variants MODIFY COLUMN weight DECIMAL(10,2) COMMENT '重量(克)';
ALTER TABLE product_variants MODIFY COLUMN barcode VARCHAR(100) COMMENT '条形码';
ALTER TABLE product_variants MODIFY COLUMN option1_name VARCHAR(100) COMMENT '选项1名称(如: Color)';
ALTER TABLE product_variants MODIFY COLUMN option1_value VARCHAR(255) COMMENT '选项1值(如: Red)';
ALTER TABLE product_variants MODIFY COLUMN option2_name VARCHAR(100) COMMENT '选项2名称(如: Size)';
ALTER TABLE product_variants MODIFY COLUMN option2_value VARCHAR(255) COMMENT '选项2值(如: Large)';
ALTER TABLE product_variants MODIFY COLUMN option3_name VARCHAR(100) COMMENT '选项3名称(如: Material)';
ALTER TABLE product_variants MODIFY COLUMN option3_value VARCHAR(255) COMMENT '选项3值(如: Cotton)';
ALTER TABLE product_variants MODIFY COLUMN sku VARCHAR(100) COMMENT 'SKU库存单位';
ALTER TABLE product_variants MODIFY COLUMN procurement_url VARCHAR(500) COMMENT '采购商品链接';
ALTER TABLE product_variants MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE product_variants MODIFY COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- product_shops 表 - 完整字段注释
ALTER TABLE product_shops MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '关联ID';
ALTER TABLE product_shops MODIFY COLUMN product_id BIGINT NOT NULL COMMENT '产品ID';
ALTER TABLE product_shops MODIFY COLUMN shop_id BIGINT NOT NULL COMMENT '商店ID';
ALTER TABLE product_shops MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- product_images 表 - 完整字段注释
ALTER TABLE product_images MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '图片ID';
ALTER TABLE product_images MODIFY COLUMN product_id BIGINT NOT NULL COMMENT '产品ID';
ALTER TABLE product_images MODIFY COLUMN image_url VARCHAR(500) NOT NULL COMMENT '图片URL';
ALTER TABLE product_images MODIFY COLUMN position INT DEFAULT 0 COMMENT '图片位置顺序';
ALTER TABLE product_images MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- sku_applications 表 - 完整字段注释
ALTER TABLE sku_applications MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '申请ID';
ALTER TABLE sku_applications MODIFY COLUMN variant_id BIGINT NOT NULL COMMENT '变体ID';
ALTER TABLE sku_applications MODIFY COLUMN requested_sku VARCHAR(100) COMMENT '申请的SKU编码';
ALTER TABLE sku_applications MODIFY COLUMN status VARCHAR(20) DEFAULT 'PENDING' COMMENT '申请状态: PENDING-待处理 APPROVED-已批准 REJECTED-已拒绝';
ALTER TABLE sku_applications MODIFY COLUMN applied_by BIGINT COMMENT '申请人用户ID';
ALTER TABLE sku_applications MODIFY COLUMN applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间';
ALTER TABLE sku_applications MODIFY COLUMN reviewed_by BIGINT COMMENT '审核人用户ID';
ALTER TABLE sku_applications MODIFY COLUMN reviewed_at TIMESTAMP COMMENT '审核时间';
ALTER TABLE sku_applications MODIFY COLUMN remarks TEXT COMMENT '备注说明';

-- liquid_schema_cache 表 - 完整字段注释
ALTER TABLE liquid_schema_cache MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '缓存ID';
ALTER TABLE liquid_schema_cache MODIFY COLUMN resource_type VARCHAR(50) NOT NULL COMMENT '资源类型(product/variant/collection等)';
ALTER TABLE liquid_schema_cache MODIFY COLUMN schema_json TEXT NOT NULL COMMENT 'Liquid Schema JSON数据';
ALTER TABLE liquid_schema_cache MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE liquid_schema_cache MODIFY COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- theme_migration_rules 表 - 完整字段注释
ALTER TABLE theme_migration_rules MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '规则ID';
ALTER TABLE theme_migration_rules MODIFY COLUMN from_version VARCHAR(50) NOT NULL COMMENT '源版本号';
ALTER TABLE theme_migration_rules MODIFY COLUMN to_version VARCHAR(50) NOT NULL COMMENT '目标版本号';
ALTER TABLE theme_migration_rules MODIFY COLUMN rule_type VARCHAR(50) NOT NULL COMMENT '规则类型';
ALTER TABLE theme_migration_rules MODIFY COLUMN rule_content TEXT NOT NULL COMMENT '规则内容(JSON格式)';
ALTER TABLE theme_migration_rules MODIFY COLUMN priority INT DEFAULT 0 COMMENT '优先级';
ALTER TABLE theme_migration_rules MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE theme_migration_rules MODIFY COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- parcels 表 - 完整字段注释
ALTER TABLE parcels MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '包裹ID';
ALTER TABLE parcels MODIFY COLUMN order_id BIGINT COMMENT '订单ID';
ALTER TABLE parcels MODIFY COLUMN tracking_number VARCHAR(100) COMMENT '运单号';
ALTER TABLE parcels MODIFY COLUMN carrier_code VARCHAR(50) COMMENT '承运商代码';
ALTER TABLE parcels MODIFY COLUMN status VARCHAR(50) COMMENT '包裹状态';
ALTER TABLE parcels MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE parcels MODIFY COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- tracking_numbers 表 - 完整字段注释
ALTER TABLE tracking_numbers MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '运单ID';
ALTER TABLE tracking_numbers MODIFY COLUMN tracking_number VARCHAR(100) NOT NULL COMMENT '运单号';
ALTER TABLE tracking_numbers MODIFY COLUMN carrier VARCHAR(50) COMMENT '承运商';
ALTER TABLE tracking_numbers MODIFY COLUMN status VARCHAR(50) COMMENT '物流状态';
ALTER TABLE tracking_numbers MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE tracking_numbers MODIFY COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- tracking_numbers_history 表 - 完整字段注释
ALTER TABLE tracking_numbers_history MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '历史记录ID';
ALTER TABLE tracking_numbers_history MODIFY COLUMN tracking_number_id BIGINT COMMENT '运单ID';
ALTER TABLE tracking_numbers_history MODIFY COLUMN status VARCHAR(50) COMMENT '物流状态';
ALTER TABLE tracking_numbers_history MODIFY COLUMN archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间';

-- tracking_webhook_logs 表 - 完整字段注释
ALTER TABLE tracking_webhook_logs MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '日志ID';
ALTER TABLE tracking_webhook_logs MODIFY COLUMN tracking_number VARCHAR(100) COMMENT '运单号';
ALTER TABLE tracking_webhook_logs MODIFY COLUMN webhook_data TEXT COMMENT 'Webhook数据';
ALTER TABLE tracking_webhook_logs MODIFY COLUMN received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间';

-- product_template_suffixes 表 - 完整字段注释
ALTER TABLE product_template_suffixes MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE product_template_suffixes MODIFY COLUMN product_id BIGINT COMMENT '产品ID';
ALTER TABLE product_template_suffixes MODIFY COLUMN suffix_value VARCHAR(255) COMMENT '模板后缀值';
ALTER TABLE product_template_suffixes MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- theme_exports 表 - 完整字段注释
ALTER TABLE theme_exports MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '导出ID';
ALTER TABLE theme_exports MODIFY COLUMN version_id BIGINT COMMENT '主题版本ID';
ALTER TABLE theme_exports MODIFY COLUMN export_path VARCHAR(500) COMMENT '导出文件路径';
ALTER TABLE theme_exports MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- theme_migration_history 表 - 完整字段注释
ALTER TABLE theme_migration_history MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '迁移历史ID';
ALTER TABLE theme_migration_history MODIFY COLUMN from_version VARCHAR(50) COMMENT '源版本';
ALTER TABLE theme_migration_history MODIFY COLUMN to_version VARCHAR(50) COMMENT '目标版本';
ALTER TABLE theme_migration_history MODIFY COLUMN status VARCHAR(50) COMMENT '迁移状态';
ALTER TABLE theme_migration_history MODIFY COLUMN migrated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '迁移时间';

-- theme_version_archive 表 - 完整字段注释
ALTER TABLE theme_version_archive MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '归档ID';
ALTER TABLE theme_version_archive MODIFY COLUMN version_id BIGINT COMMENT '主题版本ID';
ALTER TABLE theme_version_archive MODIFY COLUMN archive_path VARCHAR(500) COMMENT '归档文件路径';
ALTER TABLE theme_version_archive MODIFY COLUMN archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间';
