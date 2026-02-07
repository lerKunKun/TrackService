-- Database Comment Repair Migration Script
-- Version: V6_fix_comments
-- Date: 2026-02-03
-- Description: Fix garbled comments and add missing comments for all tables

-- 1. allowed_corp_ids
ALTER TABLE allowed_corp_ids COMMENT = '允许登录的企业CorpId白名单';
ALTER TABLE allowed_corp_ids 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `corp_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '企业CorpId',
    MODIFY COLUMN `corp_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '企业名称',
    MODIFY COLUMN `status` tinyint DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
    MODIFY COLUMN `created_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间';

-- 2. archive_logs
ALTER TABLE archive_logs COMMENT = '归档日志表';
ALTER TABLE archive_logs 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    MODIFY COLUMN `archive_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '归档类型：tracking/events',
    MODIFY COLUMN `archived_count` int NOT NULL DEFAULT '0' COMMENT '归档数量',
    MODIFY COLUMN `days_threshold` int NOT NULL COMMENT '归档天数阈值',
    MODIFY COLUMN `started_at` datetime NOT NULL COMMENT '开始时间',
    MODIFY COLUMN `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
    MODIFY COLUMN `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'running' COMMENT '状态：running/success/failed',
    MODIFY COLUMN `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错误信息';

-- 3. audit_logs
ALTER TABLE audit_logs COMMENT = '审计日志表';
ALTER TABLE audit_logs 
    MODIFY COLUMN `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    MODIFY COLUMN `user_id` bigint unsigned DEFAULT NULL COMMENT '操作用户ID',
    MODIFY COLUMN `username` varchar(50) DEFAULT NULL COMMENT '操作用户名',
    MODIFY COLUMN `operation` varchar(100) NOT NULL COMMENT '操作类型',
    MODIFY COLUMN `module` varchar(50) NOT NULL COMMENT '模块名称',
    MODIFY COLUMN `method` varchar(200) DEFAULT NULL COMMENT '方法名',
    MODIFY COLUMN `params` text COMMENT '请求参数（JSON格式）',
    MODIFY COLUMN `result` varchar(20) NOT NULL COMMENT '操作结果：SUCCESS/FAILURE',
    MODIFY COLUMN `error_msg` text COMMENT '错误信息',
    MODIFY COLUMN `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
    MODIFY COLUMN `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
    MODIFY COLUMN `execution_time` int DEFAULT NULL COMMENT '执行时长（毫秒）',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 4. carriers
ALTER TABLE carriers COMMENT = '承运商映射表';
ALTER TABLE carriers 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `carrier_id` int NOT NULL COMMENT '17Track承运商ID',
    MODIFY COLUMN `carrier_code` varchar(50) NOT NULL COMMENT '系统承运商代码',
    MODIFY COLUMN `carrier_name` varchar(100) NOT NULL COMMENT '承运商英文名称',
    MODIFY COLUMN `carrier_name_cn` varchar(100) DEFAULT NULL COMMENT '承运商中文名称',
    MODIFY COLUMN `country_id` int DEFAULT NULL COMMENT '国家ID',
    MODIFY COLUMN `country_iso` varchar(10) DEFAULT NULL COMMENT '国家ISO代码',
    MODIFY COLUMN `email` varchar(255) DEFAULT NULL COMMENT '联系邮箱',
    MODIFY COLUMN `tel` varchar(100) DEFAULT NULL COMMENT '联系电话',
    MODIFY COLUMN `url` varchar(500) DEFAULT NULL COMMENT '官网地址',
    MODIFY COLUMN `is_active` tinyint DEFAULT '1' COMMENT '是否启用',
    MODIFY COLUMN `sort_order` int DEFAULT '0' COMMENT '排序',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 5. demo_table
ALTER TABLE demo_table COMMENT = '演示表';
ALTER TABLE demo_table 
    MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `name` varchar(50) NOT NULL COMMENT '名称',
    MODIFY COLUMN `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 6. dingtalk_dept_mapping
ALTER TABLE dingtalk_dept_mapping COMMENT = '钉钉部门映射表';
ALTER TABLE dingtalk_dept_mapping 
    MODIFY COLUMN `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN `dingtalk_dept_id` bigint NOT NULL COMMENT '钉钉部门ID',
    MODIFY COLUMN `system_dept_id` bigint NOT NULL COMMENT '系统部门ID',
    MODIFY COLUMN `dingtalk_dept_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '钉钉部门名称',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 7. dingtalk_sync_logs
ALTER TABLE dingtalk_sync_logs COMMENT = '钉钉同步日志表';
ALTER TABLE dingtalk_sync_logs 
    MODIFY COLUMN `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN `sync_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '同步类型：FULL-全量，DEPT-部门，USER-用户，ROLE-角色映射',
    MODIFY COLUMN `sync_mode` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '同步模式：MANUAL-手动，AUTO-自动',
    MODIFY COLUMN `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态：RUNNING-进行中，SUCCESS-成功，FAILED-失败',
    MODIFY COLUMN `total_count` int DEFAULT '0' COMMENT '总数',
    MODIFY COLUMN `success_count` int DEFAULT '0' COMMENT '成功数',
    MODIFY COLUMN `failed_count` int DEFAULT '0' COMMENT '失败数',
    MODIFY COLUMN `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
    MODIFY COLUMN `started_at` datetime DEFAULT NULL COMMENT '开始时间',
    MODIFY COLUMN `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 8. fulfillments
ALTER TABLE fulfillments COMMENT = '订单履约表';
ALTER TABLE fulfillments 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `order_id` bigint NOT NULL COMMENT '订单ID',
    MODIFY COLUMN `shop_id` bigint NOT NULL COMMENT '店铺ID',
    MODIFY COLUMN `shopify_fulfillment_id` bigint DEFAULT NULL COMMENT 'Shopify履约ID',
    MODIFY COLUMN `tracking_number` varchar(255) NOT NULL COMMENT '运单号',
    MODIFY COLUMN `tracking_company` varchar(100) DEFAULT NULL COMMENT '物流公司',
    MODIFY COLUMN `tracking_url` varchar(500) DEFAULT NULL COMMENT '跟踪URL',
    MODIFY COLUMN `status` varchar(50) DEFAULT 'pending' COMMENT '状态',
    MODIFY COLUMN `synced_to_shopify` tinyint(1) DEFAULT '0' COMMENT '是否已同步到Shopify',
    MODIFY COLUMN `sync_error` text COMMENT '同步错误信息',
    MODIFY COLUMN `synced_at` timestamp NULL DEFAULT NULL COMMENT '同步时间',
    MODIFY COLUMN `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 9. liquid_schema_cache
ALTER TABLE liquid_schema_cache COMMENT = 'Liquid Schema缓存表';
ALTER TABLE liquid_schema_cache 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '缓存ID',
    MODIFY COLUMN `theme_name` varchar(500) NOT NULL COMMENT '主题名称',
    MODIFY COLUMN `version` varchar(50) NOT NULL COMMENT '版本号',
    MODIFY COLUMN `file_path` varchar(500) NOT NULL COMMENT '文件路径',
    MODIFY COLUMN `section_name` varchar(100) DEFAULT NULL COMMENT 'Section名称',
    MODIFY COLUMN `section_type` varchar(100) DEFAULT NULL COMMENT 'Section类型',
    MODIFY COLUMN `schema_json` text NOT NULL COMMENT 'Schema JSON内容',
    MODIFY COLUMN `settings_count` int DEFAULT '0' COMMENT '配置项数量',
    MODIFY COLUMN `settings_hash` varchar(64) DEFAULT NULL COMMENT 'Hash值',
    MODIFY COLUMN `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 10. menus
ALTER TABLE menus COMMENT = '菜单表';
ALTER TABLE menus 
    MODIFY COLUMN `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    MODIFY COLUMN `parent_id` bigint unsigned DEFAULT '0' COMMENT '父菜单ID（0为顶级菜单）',
    MODIFY COLUMN `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
    MODIFY COLUMN `menu_code` varchar(50) NOT NULL COMMENT '菜单编码（唯一标识）',
    MODIFY COLUMN `menu_type` varchar(20) NOT NULL COMMENT '菜单类型：MENU-菜单，BUTTON-按钮',
    MODIFY COLUMN `path` varchar(200) DEFAULT NULL COMMENT '路由路径',
    MODIFY COLUMN `component` varchar(200) DEFAULT NULL COMMENT '组件路径',
    MODIFY COLUMN `icon` varchar(50) DEFAULT NULL COMMENT '菜单图标',
    MODIFY COLUMN `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序（越小越靠前）',
    MODIFY COLUMN `visible` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否显示：0-隐藏，1-显示',
    MODIFY COLUMN `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 11. order_items
ALTER TABLE order_items COMMENT = '订单明细表';
ALTER TABLE order_items 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `order_id` bigint NOT NULL COMMENT '订单ID',
    MODIFY COLUMN `shopify_line_item_id` bigint DEFAULT NULL COMMENT 'Shopify行项目ID',
    MODIFY COLUMN `sku` varchar(255) DEFAULT NULL COMMENT 'SKU',
    MODIFY COLUMN `title` varchar(500) NOT NULL COMMENT '商品标题',
    MODIFY COLUMN `variant_title` varchar(255) DEFAULT NULL COMMENT '变体标题',
    MODIFY COLUMN `quantity` int NOT NULL COMMENT '数量',
    MODIFY COLUMN `price` decimal(10,2) NOT NULL COMMENT '单价',
    MODIFY COLUMN `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 12. orders
ALTER TABLE orders COMMENT = '订单表';
ALTER TABLE orders 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `shop_id` bigint NOT NULL COMMENT '店铺ID',
    MODIFY COLUMN `shopify_order_id` bigint NOT NULL COMMENT 'Shopify订单ID',
    MODIFY COLUMN `order_number` varchar(50) NOT NULL COMMENT '订单号（如1001）',
    MODIFY COLUMN `order_name` varchar(50) DEFAULT NULL COMMENT '订单名称（如#1001）',
    MODIFY COLUMN `customer_email` varchar(255) DEFAULT NULL COMMENT '客户邮箱',
    MODIFY COLUMN `customer_name` varchar(255) DEFAULT NULL COMMENT '客户姓名',
    MODIFY COLUMN `customer_phone` varchar(50) DEFAULT NULL COMMENT '客户电话',
    MODIFY COLUMN `total_price` decimal(10,2) NOT NULL COMMENT '总金额',
    MODIFY COLUMN `currency` varchar(10) NOT NULL COMMENT '货币',
    MODIFY COLUMN `financial_status` varchar(50) DEFAULT NULL COMMENT '支付状态',
    MODIFY COLUMN `fulfillment_status` varchar(50) DEFAULT NULL COMMENT '履约状态',
    MODIFY COLUMN `shipping_address_name` varchar(200) DEFAULT NULL COMMENT '收货人姓名',
    MODIFY COLUMN `shipping_address_address1` varchar(255) DEFAULT NULL COMMENT '地址行1',
    MODIFY COLUMN `shipping_address_city` varchar(100) DEFAULT NULL COMMENT '城市',
    MODIFY COLUMN `shipping_address_province` varchar(100) DEFAULT NULL COMMENT '省/州',
    MODIFY COLUMN `shipping_address_country` varchar(100) DEFAULT NULL COMMENT '国家',
    MODIFY COLUMN `shipping_address_zip` varchar(20) DEFAULT NULL COMMENT '邮编',
    MODIFY COLUMN `shipping_address_phone` varchar(50) DEFAULT NULL COMMENT '电话',
    MODIFY COLUMN `created_at` timestamp NULL DEFAULT NULL COMMENT 'Shopify订单创建时间',
    MODIFY COLUMN `updated_at` timestamp NULL DEFAULT NULL COMMENT 'Shopify订单更新时间',
    MODIFY COLUMN `synced_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
    MODIFY COLUMN `raw_data` json DEFAULT NULL COMMENT '完整订单JSON数据';

-- 13. parcels
ALTER TABLE parcels COMMENT = '包裹表';
ALTER TABLE parcels 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '包裹ID',
    MODIFY COLUMN `order_id` bigint DEFAULT NULL COMMENT '订单ID',
    MODIFY COLUMN `parcel_no` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '包裹编号',
    MODIFY COLUMN `carrier_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '承运商代码',
    MODIFY COLUMN `carrier_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '承运商名称',
    MODIFY COLUMN `shipped_at` datetime DEFAULT NULL COMMENT '发货时间',
    MODIFY COLUMN `delivered_at` datetime DEFAULT NULL COMMENT '送达时间',
    MODIFY COLUMN `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'in_transit' COMMENT '状态',
    MODIFY COLUMN `parcel_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '包裹号',
    MODIFY COLUMN `weight` decimal(8,2) DEFAULT NULL COMMENT '重量(kg)',
    MODIFY COLUMN `length` decimal(8,2) DEFAULT NULL COMMENT '长度(cm)',
    MODIFY COLUMN `width` decimal(8,2) DEFAULT NULL COMMENT '宽度(cm)',
    MODIFY COLUMN `height` decimal(8,2) DEFAULT NULL COMMENT '高度(cm)',
    MODIFY COLUMN `parcel_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '包裹状态',
    MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 14. permissions
ALTER TABLE permissions COMMENT = '权限表';
ALTER TABLE permissions 
    MODIFY COLUMN `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    MODIFY COLUMN `permission_name` varchar(100) NOT NULL COMMENT '权限名称',
    MODIFY COLUMN `permission_code` varchar(100) NOT NULL COMMENT '权限编码（唯一标识）',
    MODIFY COLUMN `permission_type` varchar(20) NOT NULL COMMENT '权限类型：MENU-菜单，BUTTON-按钮，DATA-数据',
    MODIFY COLUMN `resource_type` varchar(50) DEFAULT NULL COMMENT '资源类型',
    MODIFY COLUMN `resource_id` bigint DEFAULT NULL COMMENT '资源ID（如菜单ID）',
    MODIFY COLUMN `description` varchar(200) DEFAULT NULL COMMENT '权限描述',
    MODIFY COLUMN `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 15. product_images
ALTER TABLE product_images COMMENT = '产品图片表';
ALTER TABLE product_images 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    MODIFY COLUMN `product_id` bigint NOT NULL COMMENT '产品ID',
    MODIFY COLUMN `variant_id` bigint DEFAULT NULL COMMENT '变体ID',
    MODIFY COLUMN `src` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片URL',
    MODIFY COLUMN `alt_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '替代文本',
    MODIFY COLUMN `position` int DEFAULT '0' COMMENT '图片位置顺序',
    MODIFY COLUMN `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 16. product_imports
ALTER TABLE product_imports COMMENT = '产品导入记录表';
ALTER TABLE product_imports 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '导入任务ID',
    MODIFY COLUMN `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '导入文件名',
    MODIFY COLUMN `file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件存储路径',
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT '0' COMMENT '导入状态: 0-等待处理, 1-处理中, 2-失败, 3-成功',
    MODIFY COLUMN `total_records` int DEFAULT '0' COMMENT '总记录数',
    MODIFY COLUMN `success_records` int DEFAULT '0' COMMENT '成功记录数',
    MODIFY COLUMN `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
    MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `completed_at` datetime DEFAULT NULL COMMENT '完成时间';

-- 17. product_shops
ALTER TABLE product_shops COMMENT = '产品店铺关联表';
ALTER TABLE product_shops 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    MODIFY COLUMN `product_id` bigint NOT NULL COMMENT '产品ID',
    MODIFY COLUMN `shop_id` bigint NOT NULL COMMENT '店铺ID',
    MODIFY COLUMN `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `publish_status` tinyint DEFAULT '0' COMMENT '发布状态: 0-未发布, 1-已发布(CSV), 2-已发布(API), 3-失败',
    MODIFY COLUMN `last_export_time` datetime DEFAULT NULL COMMENT '最后导出时间',
    MODIFY COLUMN `published_by` bigint DEFAULT NULL COMMENT '发布人ID';

-- 18. product_template_suffixes
ALTER TABLE product_template_suffixes COMMENT = '产品模板后缀表';
ALTER TABLE product_template_suffixes 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `theme_name` varchar(100) NOT NULL COMMENT '主题名称',
    MODIFY COLUMN `suffix` varchar(50) NOT NULL COMMENT '后缀',
    MODIFY COLUMN `sections_snapshot` text COMMENT 'Section快照',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 19. product_variants
ALTER TABLE product_variants COMMENT = '产品变体表';
ALTER TABLE product_variants 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '变体ID',
    MODIFY COLUMN `product_id` bigint NOT NULL COMMENT '产品ID',
    MODIFY COLUMN `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变体标题',
    MODIFY COLUMN `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
    MODIFY COLUMN `compare_at_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
    MODIFY COLUMN `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变体图片URL',
    MODIFY COLUMN `inventory_quantity` int DEFAULT '0' COMMENT '库存数量',
    MODIFY COLUMN `weight` decimal(10,2) DEFAULT NULL COMMENT '重量(kg)',
    MODIFY COLUMN `barcode` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '条形码',
    MODIFY COLUMN `option1_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '选项1名称(如 Color)',
    MODIFY COLUMN `option1_value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '选项1值(如 Red)',
    MODIFY COLUMN `option2_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '选项2名称(如 Size)',
    MODIFY COLUMN `option2_value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '选项2值(如 Large)',
    MODIFY COLUMN `option3_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '选项3名称(如 Material)',
    MODIFY COLUMN `option3_value` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '选项3值(如 Cotton)',
    MODIFY COLUMN `sku` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SKU库存单位',
    MODIFY COLUMN `procurement_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '采购链接',
    MODIFY COLUMN `procurement_price` decimal(10,2) DEFAULT NULL COMMENT '采购价格',
    MODIFY COLUMN `supplier` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商',
    MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 20. product_visibility
ALTER TABLE product_visibility COMMENT = '产品可见性权限表';
ALTER TABLE product_visibility 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `product_id` bigint NOT NULL COMMENT '产品ID',
    MODIFY COLUMN `user_id` bigint DEFAULT NULL COMMENT '用户ID',
    MODIFY COLUMN `role_id` bigint unsigned DEFAULT NULL COMMENT '角色ID',
    MODIFY COLUMN `shop_id` bigint DEFAULT NULL COMMENT '店铺ID',
    MODIFY COLUMN `granted_by` bigint NOT NULL COMMENT '授权人ID',
    MODIFY COLUMN `granted_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    MODIFY COLUMN `expires_at` datetime DEFAULT NULL COMMENT '过期时间',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 21. products
ALTER TABLE products COMMENT = '产品主表';
ALTER TABLE products 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '产品ID',
    MODIFY COLUMN `handle` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品唯一标识符(用于URL)',
    MODIFY COLUMN `title` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品标题',
    MODIFY COLUMN `body_html` text COLLATE utf8mb4_unicode_ci COMMENT '产品描述(HTML格式)',
    MODIFY COLUMN `vendor` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品牌/制造商',
    MODIFY COLUMN `tags` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签(逗号分隔)',
    MODIFY COLUMN `procurement_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '采购链接',
    MODIFY COLUMN `procurement_price` decimal(10,2) DEFAULT NULL COMMENT '采购价格',
    MODIFY COLUMN `supplier` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商',
    MODIFY COLUMN `published` tinyint DEFAULT '0' COMMENT '上架状态: 0-草稿 1-已上架',
    MODIFY COLUMN `product_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '产品URL',
    MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 22. role_mapping_rules
ALTER TABLE role_mapping_rules COMMENT = '角色映射规则表';
ALTER TABLE role_mapping_rules 
    MODIFY COLUMN `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
    MODIFY COLUMN `rule_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则类型：DEPT-按部门，TITLE-按职位，DEPT_TITLE-部门+职位',
    MODIFY COLUMN `dingtalk_dept_id` bigint DEFAULT NULL COMMENT '钉钉部门ID（rule_type为DEPT或DEPT_TITLE时必填）',
    MODIFY COLUMN `dingtalk_title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '钉钉职位名称（rule_type为TITLE或DEPT_TITLE时必填）',
    MODIFY COLUMN `role_id` bigint unsigned NOT NULL COMMENT '系统角色ID（关联roles表）',
    MODIFY COLUMN `priority` int DEFAULT '0' COMMENT '优先级（数字越大优先级越高）',
    MODIFY COLUMN `status` tinyint(1) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 23. role_menus
ALTER TABLE role_menus COMMENT = '角色菜单关联表';
ALTER TABLE role_menus 
    MODIFY COLUMN `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
    MODIFY COLUMN `menu_id` bigint unsigned NOT NULL COMMENT '菜单ID',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 24. role_permissions
ALTER TABLE role_permissions COMMENT = '角色权限关联表';
ALTER TABLE role_permissions 
    MODIFY COLUMN `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
    MODIFY COLUMN `permission_id` bigint unsigned NOT NULL COMMENT '权限ID',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 25. roles
ALTER TABLE roles COMMENT = '角色表';
ALTER TABLE roles 
    MODIFY COLUMN `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    MODIFY COLUMN `role_name` varchar(50) NOT NULL COMMENT '角色名称',
    MODIFY COLUMN `role_code` varchar(50) NOT NULL COMMENT '角色编码（唯一标识）',
    MODIFY COLUMN `description` varchar(200) DEFAULT NULL COMMENT '角色描述',
    MODIFY COLUMN `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 26. shops
ALTER TABLE shops COMMENT = '店铺表';
ALTER TABLE shops 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '店铺ID',
    MODIFY COLUMN `user_id` bigint NOT NULL COMMENT '所属用户ID',
    MODIFY COLUMN `shop_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '店铺名称',
    MODIFY COLUMN `platform` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台类型：shopify, shopline, tiktok',
    MODIFY COLUMN `shop_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '店铺URL',
    MODIFY COLUMN `shop_domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Shopify店铺域名(xxx.myshopify.com)',
    MODIFY COLUMN `timezone` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '店铺时区',
    MODIFY COLUMN `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API密钥',
    MODIFY COLUMN `api_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API密钥Secret',
    MODIFY COLUMN `access_token` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '访问令牌',
    MODIFY COLUMN `token_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'offline' COMMENT 'Token类型：offline(永久), online(24小时)',
    MODIFY COLUMN `connection_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'active' COMMENT '连接状态：active(正常), invalid(失效), pending(待授权)',
    MODIFY COLUMN `last_validated_at` datetime DEFAULT NULL COMMENT '最后验证时间',
    MODIFY COLUMN `webhook_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Webhook密钥',
    MODIFY COLUMN `oauth_state` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'OAuth state nonce',
    MODIFY COLUMN `oauth_scope` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'OAuth授权范围',
    MODIFY COLUMN `token_expires_at` datetime DEFAULT NULL COMMENT 'Token过期时间',
    MODIFY COLUMN `is_active` tinyint NOT NULL DEFAULT '1' COMMENT '是否激活：0-否，1-是',
    MODIFY COLUMN `last_sync_time` datetime DEFAULT NULL COMMENT '最后同步时间',
    MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
    MODIFY COLUMN `contact_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '店铺联系邮箱',
    MODIFY COLUMN `owner_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '店主邮箱',
    MODIFY COLUMN `currency` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '店铺货币代码',
    MODIFY COLUMN `plan_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订阅计划名称',
    MODIFY COLUMN `plan_display_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '计划显示名称',
    MODIFY COLUMN `is_shopify_plus` tinyint(1) DEFAULT '0' COMMENT '是否为Shopify Plus',
    MODIFY COLUMN `primary_domain` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主域名',
    MODIFY COLUMN `shop_owner` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '店主姓名',
    MODIFY COLUMN `iana_timezone` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IANA时区标识',
    MODIFY COLUMN `shop_info_json` text COLLATE utf8mb4_unicode_ci COMMENT '完整店铺信息JSON（用于扩展存储）';

-- 27. sku_applications
ALTER TABLE sku_applications COMMENT = 'SKU申请表';
ALTER TABLE sku_applications 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    MODIFY COLUMN `product_variant_id` bigint NOT NULL COMMENT '产品变体ID',
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态: 0-待审核, 1-已批准, 2-已拒绝',
    MODIFY COLUMN `generated_sku` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成的SKU',
    MODIFY COLUMN `applied_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    MODIFY COLUMN `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间';

-- 28. sync_jobs
ALTER TABLE sync_jobs COMMENT = '同步任务表';
ALTER TABLE sync_jobs 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    MODIFY COLUMN `job_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务类型：order_sync, tracking_sync',
    MODIFY COLUMN `shop_id` bigint DEFAULT NULL COMMENT '店铺ID',
    MODIFY COLUMN `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务状态：pending, running, completed, failed',
    MODIFY COLUMN `total_count` int DEFAULT '0' COMMENT '总数',
    MODIFY COLUMN `success_count` int DEFAULT '0' COMMENT '成功数',
    MODIFY COLUMN `failed_count` int DEFAULT '0' COMMENT '失败数',
    MODIFY COLUMN `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
    MODIFY COLUMN `started_at` datetime DEFAULT NULL COMMENT '开始时间',
    MODIFY COLUMN `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
    MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 29. theme_exports
ALTER TABLE theme_exports COMMENT = '主题导出记录表';
ALTER TABLE theme_exports 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `theme_name` varchar(100) NOT NULL COMMENT '主题名称',
    MODIFY COLUMN `version` varchar(20) NOT NULL COMMENT '版本号',
    MODIFY COLUMN `export_path` varchar(500) NOT NULL COMMENT '导出路径',
    MODIFY COLUMN `file_size` bigint DEFAULT NULL COMMENT '文件大小',
    MODIFY COLUMN `exported_by` varchar(50) DEFAULT NULL COMMENT '导出人',
    MODIFY COLUMN `exported_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '导出时间';

-- 30. theme_migration_history
ALTER TABLE theme_migration_history COMMENT = '主题迁移历史表';
ALTER TABLE theme_migration_history 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `theme_name` varchar(100) NOT NULL COMMENT '主题名称',
    MODIFY COLUMN `from_version` varchar(20) NOT NULL COMMENT '源版本',
    MODIFY COLUMN `to_version` varchar(20) NOT NULL COMMENT '目标版本',
    MODIFY COLUMN `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态',
    MODIFY COLUMN `templates_updated` int DEFAULT '0' COMMENT '更新模板数',
    MODIFY COLUMN `executed_by` varchar(50) DEFAULT NULL COMMENT '执行人',
    MODIFY COLUMN `executed_at` datetime DEFAULT NULL COMMENT '执行时间',
    MODIFY COLUMN `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
    MODIFY COLUMN `error_message` text COMMENT '错误信息',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 31. theme_migration_rules
ALTER TABLE theme_migration_rules COMMENT = '主题迁移规则表';
ALTER TABLE theme_migration_rules 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN `theme_name` varchar(100) NOT NULL COMMENT '主题名称',
    MODIFY COLUMN `from_version` varchar(50) NOT NULL COMMENT '源版本号',
    MODIFY COLUMN `to_version` varchar(50) NOT NULL COMMENT '目标版本号',
    MODIFY COLUMN `rule_type` varchar(50) NOT NULL COMMENT '规则类型',
    MODIFY COLUMN `section_name` varchar(100) DEFAULT NULL COMMENT 'Section名称',
    MODIFY COLUMN `rule_json` text NOT NULL COMMENT 'JSON规则配置',
    MODIFY COLUMN `confidence` varchar(20) DEFAULT NULL COMMENT '置信度',
    MODIFY COLUMN `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `created_by` varchar(100) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 32. theme_version_archive
ALTER TABLE theme_version_archive COMMENT = '主题版本归档表';
ALTER TABLE theme_version_archive 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `theme_name` varchar(100) NOT NULL COMMENT '主题名称',
    MODIFY COLUMN `version` varchar(20) NOT NULL COMMENT '版本',
    MODIFY COLUMN `zip_file_path` varchar(500) NOT NULL COMMENT 'ZIP文件路径',
    MODIFY COLUMN `zip_file_size` bigint DEFAULT NULL COMMENT 'ZIP文件大小',
    MODIFY COLUMN `sections_count` int DEFAULT NULL COMMENT 'Section数量',
    MODIFY COLUMN `is_current` tinyint(1) DEFAULT '0' COMMENT '是否当前版本',
    MODIFY COLUMN `uploaded_by` varchar(50) DEFAULT NULL COMMENT '上传人',
    MODIFY COLUMN `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 33. tracking_events
ALTER TABLE tracking_events COMMENT = '物流事件表';
ALTER TABLE tracking_events 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件ID',
    MODIFY COLUMN `tracking_id` bigint NOT NULL COMMENT '运单ID',
    MODIFY COLUMN `event_time` datetime NOT NULL COMMENT '事件时间',
    MODIFY COLUMN `event_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '事件描述',
    MODIFY COLUMN `event_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '事件地点',
    MODIFY COLUMN `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '城市',
    MODIFY COLUMN `postal_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮编',
    MODIFY COLUMN `provider_key` int DEFAULT NULL COMMENT '承运商ID',
    MODIFY COLUMN `provider_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '承运商名称',
    MODIFY COLUMN `event_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '事件代码',
    MODIFY COLUMN `stage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '阶段(PickedUp/InTransit/Delivered等)',
    MODIFY COLUMN `sub_status` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子状态',
    MODIFY COLUMN `time_iso` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ISO时间字符串',
    MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 34. tracking_events_history
ALTER TABLE tracking_events_history COMMENT = '历史物流事件表';
ALTER TABLE tracking_events_history 
    MODIFY COLUMN `id` bigint NOT NULL COMMENT '事件ID',
    MODIFY COLUMN `tracking_id` bigint NOT NULL COMMENT '运单ID',
    MODIFY COLUMN `event_time` datetime NOT NULL COMMENT '事件时间',
    MODIFY COLUMN `event_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '事件描述',
    MODIFY COLUMN `event_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '事件地点',
    MODIFY COLUMN `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '城市',
    MODIFY COLUMN `postal_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮编',
    MODIFY COLUMN `provider_key` int DEFAULT NULL COMMENT '承运商ID',
    MODIFY COLUMN `provider_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '承运商名称',
    MODIFY COLUMN `event_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '事件代码',
    MODIFY COLUMN `stage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '阶段',
    MODIFY COLUMN `sub_status` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子状态',
    MODIFY COLUMN `time_iso` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ISO时间字符串',
    MODIFY COLUMN `created_at` datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN `archived_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间';

-- 35. tracking_numbers
ALTER TABLE tracking_numbers COMMENT = '运单表';
ALTER TABLE tracking_numbers 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '运单ID',
    MODIFY COLUMN `user_id` bigint DEFAULT NULL COMMENT '用户ID',
    MODIFY COLUMN `parcel_id` bigint DEFAULT NULL COMMENT '包裹ID（可为空，手动添加的运单）',
    MODIFY COLUMN `tracking_number` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '运单号',
    MODIFY COLUMN `carrier_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '承运商代码',
    MODIFY COLUMN `carrier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '承运商名称',
    MODIFY COLUMN `carrier_id` int DEFAULT NULL COMMENT '17Track承运商ID',
    MODIFY COLUMN `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源',
    MODIFY COLUMN `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
    MODIFY COLUMN `track_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单状态',
    MODIFY COLUMN `sub_status` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子状态',
    MODIFY COLUMN `sub_status_descr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子状态描述',
    MODIFY COLUMN `days_of_transit` int DEFAULT NULL COMMENT '运输天数',
    MODIFY COLUMN `days_after_last_update` int DEFAULT NULL COMMENT '距离最后更新天数',
    MODIFY COLUMN `latest_event_time` datetime DEFAULT NULL COMMENT '最新事件时间',
    MODIFY COLUMN `latest_event_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最新事件描述',
    MODIFY COLUMN `latest_event_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最新事件地点',
    MODIFY COLUMN `pickup_time` datetime DEFAULT NULL COMMENT '揽收时间',
    MODIFY COLUMN `delivered_time` datetime DEFAULT NULL COMMENT '签收时间',
    MODIFY COLUMN `destination_country` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目的地国家',
    MODIFY COLUMN `origin_country` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '始发国家代码',
    MODIFY COLUMN `package_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '包裹状态',
    MODIFY COLUMN `track17_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '17Track状态码',
    MODIFY COLUMN `track17_substatus` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '17Track子状态码',
    MODIFY COLUMN `latest_event` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '最新物流事件',
    MODIFY COLUMN `registered_at_17track` tinyint DEFAULT '0' COMMENT '是否已注册到17Track：0-否，1-是',
    MODIFY COLUMN `last_sync_time` datetime DEFAULT NULL COMMENT '最后同步时间',
    MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN `version` int NOT NULL DEFAULT '0' COMMENT '版本号（乐观锁）',
    MODIFY COLUMN `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）';

-- 36. tracking_numbers_history
ALTER TABLE tracking_numbers_history COMMENT = '历史运单表';
ALTER TABLE tracking_numbers_history 
    MODIFY COLUMN `id` bigint NOT NULL COMMENT '运单ID',
    MODIFY COLUMN `user_id` bigint DEFAULT NULL COMMENT '用户ID',
    MODIFY COLUMN `parcel_id` bigint DEFAULT NULL COMMENT '包裹ID',
    MODIFY COLUMN `tracking_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '运单号',
    MODIFY COLUMN `carrier_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '承运商代码',
    MODIFY COLUMN `carrier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '承运商名称',
    MODIFY COLUMN `carrier_id` int DEFAULT NULL COMMENT '17Track承运商ID',
    MODIFY COLUMN `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源',
    MODIFY COLUMN `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
    MODIFY COLUMN `track_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '追踪状态',
    MODIFY COLUMN `sub_status` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子状态',
    MODIFY COLUMN `sub_status_descr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子状态描述',
    MODIFY COLUMN `days_of_transit` int DEFAULT NULL COMMENT '运输天数',
    MODIFY COLUMN `days_after_last_update` int DEFAULT NULL COMMENT '距离最后更新天数',
    MODIFY COLUMN `latest_event_time` datetime DEFAULT NULL COMMENT '最新事件时间',
    MODIFY COLUMN `latest_event_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最新事件描述',
    MODIFY COLUMN `latest_event_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最新事件地点',
    MODIFY COLUMN `pickup_time` datetime DEFAULT NULL COMMENT '揽收时间',
    MODIFY COLUMN `delivered_time` datetime DEFAULT NULL COMMENT '签收时间',
    MODIFY COLUMN `destination_country` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目的地国家',
    MODIFY COLUMN `origin_country` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '始发国家代码',
    MODIFY COLUMN `package_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '包裹状态',
    MODIFY COLUMN `track17_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '17Track状态码',
    MODIFY COLUMN `track17_substatus` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '17Track子状态码',
    MODIFY COLUMN `latest_event` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '最新物流事件',
    MODIFY COLUMN `registered_at_17track` tinyint DEFAULT '0' COMMENT '是否已注册到17Track',
    MODIFY COLUMN `last_sync_time` datetime DEFAULT NULL COMMENT '最后同步时间',
    MODIFY COLUMN `created_at` datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime NOT NULL COMMENT '更新时间',
    MODIFY COLUMN `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
    MODIFY COLUMN `archived_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间';

-- 37. tracking_webhook_logs
ALTER TABLE tracking_webhook_logs COMMENT = '物流Webhook日志表';
ALTER TABLE tracking_webhook_logs 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    MODIFY COLUMN `tracking_id` bigint DEFAULT NULL COMMENT '运单ID',
    MODIFY COLUMN `payload` json DEFAULT NULL COMMENT 'Webhook数据',
    MODIFY COLUMN `received_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间';

-- 38. user_roles
ALTER TABLE user_roles COMMENT = '用户角色关联表';
ALTER TABLE user_roles 
    MODIFY COLUMN `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
    MODIFY COLUMN `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 39. user_shop_roles
ALTER TABLE user_shop_roles COMMENT = '用户店铺角色关联表';
ALTER TABLE user_shop_roles 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    MODIFY COLUMN `user_id` bigint NOT NULL COMMENT '用户ID',
    MODIFY COLUMN `shop_id` bigint NOT NULL COMMENT '店铺ID',
    MODIFY COLUMN `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
    MODIFY COLUMN `granted_by` bigint DEFAULT NULL COMMENT '授权人',
    MODIFY COLUMN `granted_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    MODIFY COLUMN `expires_at` datetime DEFAULT NULL COMMENT '过期时间',
    MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 40. users
ALTER TABLE users COMMENT = '用户表';
ALTER TABLE users 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    MODIFY COLUMN `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
    MODIFY COLUMN `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '密码（BCrypt加密）',
    MODIFY COLUMN `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
    MODIFY COLUMN `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电话',
    MODIFY COLUMN `real_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '真实姓名',
    MODIFY COLUMN `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN, USER',
    MODIFY COLUMN `status` tinyint DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
    MODIFY COLUMN `sync_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用自动同步：0-禁用，1-启用',
    MODIFY COLUMN `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
    MODIFY COLUMN `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    MODIFY COLUMN `last_login_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后登录IP',
    MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
    MODIFY COLUMN `ding_union_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '钉钉UnionID',
    MODIFY COLUMN `ding_userid` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '钉钉userId（企业内唯一标识）',
    MODIFY COLUMN `job_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工号',
    MODIFY COLUMN `title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职位',
    MODIFY COLUMN `ding_user_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '钉钉userId（此字段与ding_userid重复，建议统一）',
    MODIFY COLUMN `corp_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '企业CorpId',
    MODIFY COLUMN `login_source` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PASSWORD' COMMENT '登录来源：PASSWORD, DINGTALK';

-- 41. webhook_logs
ALTER TABLE webhook_logs COMMENT = 'Webhook日志表';
ALTER TABLE webhook_logs 
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    MODIFY COLUMN `shop_id` bigint DEFAULT NULL COMMENT '店铺ID',
    MODIFY COLUMN `webhook_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Webhook类型',
    MODIFY COLUMN `payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '请求体（JSON）',
    MODIFY COLUMN `headers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '请求头（JSON）',
    MODIFY COLUMN `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理状态：success, failed',
    MODIFY COLUMN `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
    MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

