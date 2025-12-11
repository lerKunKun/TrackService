-- 创建订单相关表
-- 版本: V3

-- 1. 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id BIGINT NOT NULL COMMENT '店铺ID',
    shopify_order_id BIGINT NOT NULL COMMENT 'Shopify订单ID',
    order_number VARCHAR(50) NOT NULL COMMENT '订单号（如1001）',
    order_name VARCHAR(50) COMMENT '订单名称（如#1001）',
    
    -- 客户信息
    customer_email VARCHAR(255) COMMENT '客户邮箱',
    customer_name VARCHAR(255) COMMENT '客户姓名',
    customer_phone VARCHAR(50) COMMENT '客户电话',
    
    -- 金额信息
    total_price DECIMAL(10,2) NOT NULL COMMENT '总金额',
    currency VARCHAR(10) NOT NULL COMMENT '货币',
    
    -- 状态信息
    financial_status VARCHAR(50) COMMENT '支付状态: paid, pending, refunded等',
    fulfillment_status VARCHAR(50) COMMENT '履约状态: fulfilled, partial, null',
    
    -- 收货地址
    shipping_address_name VARCHAR(200) COMMENT '收货人姓名',
    shipping_address_address1 VARCHAR(255) COMMENT '地址行1',
    shipping_address_city VARCHAR(100) COMMENT '城市',
    shipping_address_province VARCHAR(100) COMMENT '省/州',
    shipping_address_country VARCHAR(100) COMMENT '国家',
    shipping_address_zip VARCHAR(20) COMMENT '邮编',
    shipping_address_phone VARCHAR(50) COMMENT '电话',
    
    -- 时间信息
    created_at TIMESTAMP NULL COMMENT 'Shopify订单创建时间',
    updated_at TIMESTAMP NULL COMMENT 'Shopify订单更新时间',
    synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
    
    -- 原始数据
    raw_data JSON COMMENT '完整订单JSON数据',
    
    -- 索引
    INDEX idx_shop_id (shop_id),
    INDEX idx_order_number (order_number),
    INDEX idx_customer_email (customer_email),
    INDEX idx_created_at (created_at),
    UNIQUE KEY uk_shop_shopify_order (shop_id, shopify_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Shopify订单表';

-- 2. 订单商品表
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    shopify_line_item_id BIGINT COMMENT 'Shopify行项目ID',
    
    -- 商品信息
    sku VARCHAR(255) COMMENT 'SKU',
    title VARCHAR(500) NOT NULL COMMENT '商品标题',
    variant_title VARCHAR(255) COMMENT '变体标题',
    
    -- 数量和价格
    quantity INT NOT NULL COMMENT '数量',
    price DECIMAL(10,2) NOT NULL COMMENT '单价',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_order_id (order_id),
    INDEX idx_sku (sku),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品明细表';

-- 3. 履约表
CREATE TABLE IF NOT EXISTS fulfillments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    shop_id BIGINT NOT NULL COMMENT '店铺ID',
    shopify_fulfillment_id BIGINT COMMENT 'Shopify履约ID',
    
    -- 物流信息
    tracking_number VARCHAR(255) NOT NULL COMMENT '运单号',
    tracking_company VARCHAR(100) COMMENT '物流公司',
    tracking_url VARCHAR(500) COMMENT '跟踪URL',
    
    -- 状态
    status VARCHAR(50) DEFAULT 'pending' COMMENT '状态: pending, success, failure',
    
    -- 同步信息
    synced_to_shopify BOOLEAN DEFAULT FALSE COMMENT '是否已同步到Shopify',
    sync_error TEXT COMMENT '同步错误信息',
    synced_at TIMESTAMP NULL COMMENT '同步时间',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_order_id (order_id),
    INDEX idx_shop_id (shop_id),
    INDEX idx_tracking_number (tracking_number),
    INDEX idx_status (status),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='履约（运单）表';
