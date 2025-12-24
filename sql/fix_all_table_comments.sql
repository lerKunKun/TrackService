-- ========== 完整修复所有数据库表注释乱码 ==========
-- 执行方式: 复制每段SQL到MySQL客户端执行

-- 系统核心表
ALTER TABLE allowed_corp_ids COMMENT '允许登录的企业CorpId白名单';
ALTER TABLE archive_logs COMMENT '归档日志表';
ALTER TABLE carriers COMMENT '承运商映射表';
ALTER TABLE dingtalk_dept_mapping COMMENT '钉钉部门映射表';
ALTER TABLE dingtalk_sync_logs COMMENT '钉钉同步日志表';
ALTER TABLE liquid_schema_cache COMMENT 'Liquid Schema缓存表';
ALTER TABLE menus COMMENT '菜单表';
ALTER TABLE order_items COMMENT '订单明细表';
ALTER TABLE orders COMMENT '订单表';
ALTER TABLE parcels COMMENT '包裹表';
ALTER TABLE permissions COMMENT '权限表';

-- 产品相关表
ALTER TABLE product_images COMMENT '产品图片表';
ALTER TABLE product_imports COMMENT '产品导入记录表';
ALTER TABLE product_shops COMMENT '产品商店关联表';
ALTER TABLE product_variants COMMENT '产品变体表';
ALTER TABLE products COMMENT '产品主表';
ALTER TABLE sku_applications COMMENT 'SKU申请表';

-- 角色权限相关表
ALTER TABLE role_mapping_rules COMMENT '角色映射规则表';
ALTER TABLE role_menus COMMENT '角色菜单关联表';
ALTER TABLE role_permissions COMMENT '角色权限关联表';
ALTER TABLE roles COMMENT '角色表';

-- 店铺和同步相关表
ALTER TABLE shops COMMENT '店铺表';
ALTER TABLE sync_jobs COMMENT '同步任务表';

-- 主题相关表  
ALTER TABLE theme_migration_rules COMMENT '主题迁移规则表';

-- 物流追踪相关表
ALTER TABLE tracking_events COMMENT '物流事件表';
ALTER TABLE tracking_events_history COMMENT '历史物流事件表';
ALTER TABLE tracking_numbers COMMENT '运单表';
ALTER TABLE tracking_numbers_history COMMENT '历史运单表';

-- 用户相关表
ALTER TABLE user_roles COMMENT '用户角色关联表';
ALTER TABLE users COMMENT '用户表';

-- Webhook日志表
ALTER TABLE webhook_logs COMMENT 'Webhook日志表';
ALTER TABLE tracking_webhook_logs COMMENT '物流Webhook日志表';

-- 钉钉相关表
ALTER TABLE dingtalk_departments COMMENT '钉钉部门表';
ALTER TABLE dingtalk_users COMMENT '钉钉用户表';

-- 审计日志
ALTER TABLE audit_logs COMMENT '审计日志表';

-- 主题相关其他表
ALTER TABLE theme_versions COMMENT '主题版本表';
