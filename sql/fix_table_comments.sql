-- 修复数据库表注释乱码
-- 使用UTF-8编码执行此脚本

-- 产品相关表
ALTER TABLE products COMMENT '产品主表';
ALTER TABLE product_variants COMMENT '产品变体表';
ALTER TABLE product_shops COMMENT '产品商店关联表';
ALTER TABLE product_imports COMMENT '产品导入记录表';

-- 用户和权限相关表
ALTER TABLE users COMMENT '用户表';
ALTER TABLE roles COMMENT '角色表';
ALTER TABLE permissions COMMENT '权限表';
ALTER TABLE user_roles COMMENT '用户角色关联表';
ALTER TABLE role_permissions COMMENT '角色权限关联表';
ALTER TABLE menus COMMENT '菜单表';
ALTER TABLE role_menus COMMENT '角色菜单关联表';

-- 店铺和订单相关表
ALTER TABLE shops COMMENT '店铺表';
ALTER TABLE orders COMMENT '订单表';
ALTER TABLE order_items COMMENT '订单明细表';

-- 钉钉相关表
ALTER TABLE dingtalk_departments COMMENT '钉钉部门表';
ALTER TABLE dingtalk_users COMMENT '钉钉用户表';
ALTER TABLE allowed_corp_ids COMMENT '允许的企业CorpID表';

-- 主题迁移相关表
ALTER TABLE theme_versions COMMENT '主题版本表';
ALTER TABLE theme_migration_rules COMMENT '主题迁移规则表';
ALTER TABLE liquid_schema_cache COMMENT 'Liquid模式缓存表';

-- 系统日志表
ALTER TABLE audit_logs COMMENT '审计日志表';
