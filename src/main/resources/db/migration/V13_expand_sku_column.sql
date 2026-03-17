-- V13: 扩展 product_variants.sku 列长度，解决导入长SKU时截断报错
ALTER TABLE `product_variants`
    MODIFY COLUMN `sku` VARCHAR(500) COMMENT 'SKU库存单位';

-- 同步更新 sku_applications.generated_sku 列
ALTER TABLE `sku_applications`
    MODIFY COLUMN `generated_sku` VARCHAR(500) COMMENT '生成的SKU';
