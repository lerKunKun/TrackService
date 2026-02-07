package com.logistics.track17.service;

import com.logistics.track17.entity.Product;
import com.logistics.track17.entity.ProductVariant;
import com.logistics.track17.mapper.ProductMapper;
import com.logistics.track17.mapper.ProductShopMapper;
import com.logistics.track17.mapper.ProductVariantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 产品刊登服务
 */
@Slf4j
@Service
public class ProductListingService {

    private final ProductMapper productMapper;
    private final ProductVariantMapper productVariantMapper;
    private final ProductShopMapper productShopMapper;

    public ProductListingService(ProductMapper productMapper,
            ProductVariantMapper productVariantMapper,
            ProductShopMapper productShopMapper) {
        this.productMapper = productMapper;
        this.productVariantMapper = productVariantMapper;
        this.productShopMapper = productShopMapper;
    }

    /**
     * 导出Shopify格式CSV
     * 
     * @param productIds 产品ID列表
     * @return CSV文件内容字节数组
     */
    @Transactional
    public byte[] exportShopifyCsv(List<Long> productIds) {
        StringBuilder csv = new StringBuilder();

        // CSV Header
        csv.append("Handle,Title,Body (HTML),Vendor,Tags,Published,");
        csv.append("Option1 Name,Option1 Value,Option2 Name,Option2 Value,Option3 Name,Option3 Value,");
        csv.append("Variant SKU,Variant Grams,Variant Inventory Tracker,Variant Inventory Qty,");
        csv.append("Variant Inventory Policy,Variant Fulfillment Service,Variant Price,Variant Compare At Price,");
        csv.append("Variant Requires Shipping,Variant Taxable,Variant Barcode,Image Src,Image Position,");
        csv.append("Image Alt Text,Gift Card,SEO Title,SEO Description,");
        csv.append("Google Shopping / Google Product Category,Google Shopping / Gender,Google Shopping / Age Group,");
        csv.append("Google Shopping / MPN,Google Shopping / AdWords Grouping,Google Shopping / AdWords Labels,");
        csv.append("Google Shopping / Condition,Google Shopping / Custom Product,Google Shopping / Custom Label 0,");
        csv.append(
                "Google Shopping / Custom Label 1,Google Shopping / Custom Label 2,Google Shopping / Custom Label 3,");
        csv.append(
                "Google Shopping / Custom Label 4,Variant Image,Variant Weight Unit,Variant Tax Code,Cost per item,Status\n");

        if (productIds == null || productIds.isEmpty()) {
            return csv.toString().getBytes(StandardCharsets.UTF_8);
        }

        List<ProductStoreStatusUpdate> updates = new ArrayList<>();

        for (Long productId : productIds) {
            Product product = productMapper.selectById(productId);
            if (product == null)
                continue;

            List<ProductVariant> variants = productVariantMapper.selectByProductId(productId);
            if (variants == null || variants.isEmpty())
                continue;

            // 1. 生成CSV内容
            processProductToCsv(csv, product, variants);

            // 2. 收集需要更新状态的记录
            // 注意：这里假设导出操作关联到该产品已绑定的所有店铺，或者我们应该有一个当前上下文的shopId？
            // MVP中，产品可能还未绑定Shop，或者绑定多个。
            // 简单起见，我们将所有关联的 product_shop 记录更新为 "已导出"
            List<Long> shopIds = productShopMapper.selectShopIdsByProductId(productId);
            for (Long shopId : shopIds) {
                updates.add(new ProductStoreStatusUpdate(productId, shopId));
            }
        }

        // 更新状态
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = com.logistics.track17.util.UserContextHolder.getCurrentUserId();
        for (ProductStoreStatusUpdate update : updates) {
            productShopMapper.updatePublishStatus(update.productId, update.shopId, 1, now, currentUserId); // 1 = 已导出
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void processProductToCsv(StringBuilder csv, Product product, List<ProductVariant> variants) {
        // 第一行: 产品信息 + 第一个变体
        ProductVariant firstVariant = variants.get(0);
        appendCsvField(csv, product.getHandle());
        csv.append(",");
        appendCsvField(csv, product.getTitle());
        csv.append(",");
        appendCsvField(csv, product.getBodyHtml());
        csv.append(",");
        appendCsvField(csv, product.getVendor());
        csv.append(",");
        appendCsvField(csv, product.getTags());
        csv.append(",");
        csv.append(product.getPublished() != null && product.getPublished() == 1 ? "TRUE" : "FALSE").append(",");

        // 变体选项
        appendCsvField(csv, firstVariant.getOption1Name());
        csv.append(",");
        appendCsvField(csv, firstVariant.getOption1Value());
        csv.append(",");
        appendCsvField(csv, firstVariant.getOption2Name());
        csv.append(",");
        appendCsvField(csv, firstVariant.getOption2Value());
        csv.append(",");
        appendCsvField(csv, firstVariant.getOption3Name());
        csv.append(",");
        appendCsvField(csv, firstVariant.getOption3Value());
        csv.append(",");

        // 变体信息
        appendCsvField(csv, firstVariant.getSku());
        csv.append(",");
        csv.append(firstVariant.getWeight() != null ? firstVariant.getWeight() : "").append(",");
        csv.append("shopify,"); // Inventory Tracker
        csv.append(firstVariant.getInventoryQuantity() != null ? firstVariant.getInventoryQuantity() : "0").append(",");
        csv.append("deny,manual,");
        csv.append(firstVariant.getPrice() != null ? firstVariant.getPrice() : "").append(",");
        csv.append(firstVariant.getCompareAtPrice() != null ? firstVariant.getCompareAtPrice() : "").append(",");
        csv.append("TRUE,TRUE,");
        appendCsvField(csv, firstVariant.getBarcode());
        csv.append(",");
        appendCsvField(csv, firstVariant.getImageUrl());
        csv.append(",");
        csv.append("1,"); // Image Position
        csv.append(","); // Alt Text
        csv.append("FALSE,"); // Gift Card

        // SEO & Google Shopping (Empty)
        for (int i = 0; i < 20; i++)
            csv.append(",");

        appendCsvField(csv, firstVariant.getImageUrl()); // Variant Image
        csv.append(",");
        csv.append("g,"); // Weight Unit
        csv.append(","); // Tax Code
        csv.append(","); // Cost per item
        csv.append(product.getPublished() != null && product.getPublished() == 1 ? "active" : "draft");
        csv.append("\n");

        // 后续行: 其他变体
        for (int i = 1; i < variants.size(); i++) {
            ProductVariant variant = variants.get(i);
            appendCsvField(csv, product.getHandle());
            csv.append(",");
            csv.append(",,,,,,"); // Title..Published Empty

            // Options
            appendCsvField(csv, variant.getOption1Name());
            csv.append(",");
            appendCsvField(csv, variant.getOption1Value());
            csv.append(",");
            appendCsvField(csv, variant.getOption2Name());
            csv.append(",");
            appendCsvField(csv, variant.getOption2Value());
            csv.append(",");
            appendCsvField(csv, variant.getOption3Name());
            csv.append(",");
            appendCsvField(csv, variant.getOption3Value());
            csv.append(",");

            // Variant Info
            appendCsvField(csv, variant.getSku());
            csv.append(",");
            csv.append(variant.getWeight() != null ? variant.getWeight() : "").append(",");
            csv.append("shopify,");
            csv.append(variant.getInventoryQuantity() != null ? variant.getInventoryQuantity() : "0").append(",");
            csv.append("deny,manual,");
            csv.append(variant.getPrice() != null ? variant.getPrice() : "").append(",");
            csv.append(variant.getCompareAtPrice() != null ? variant.getCompareAtPrice() : "").append(",");
            csv.append("TRUE,TRUE,");
            appendCsvField(csv, variant.getBarcode());
            csv.append(",");
            appendCsvField(csv, variant.getImageUrl());
            csv.append(",");
            csv.append((i + 1)).append(","); // Image Position

            // Empty fields
            for (int j = 0; j < 21; j++)
                csv.append(",");

            // ... (Variant Image etc)
            // Fix: The previous loop logic in ProductService seemed to append trailing
            // commas correctly?
            // Let's match the first row structure.
            // SEO & Google Shopping lines are empty

            appendCsvField(csv, variant.getImageUrl());
            csv.append(",g,,,"); // Unit, Tax, Cost, Status(can be empty for variants but usually inherits)
            csv.append("\n");
        }
    }

    private void appendCsvField(StringBuilder csv, String field) {
        if (field == null) {
            return;
        }
        // 如果包含逗号、双引号或换行符，需要用双引号包围，并将内部双引号转义
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            csv.append("\"");
            csv.append(field.replace("\"", "\"\""));
            csv.append("\"");
        } else {
            csv.append(field);
        }
    }

    // 内部类用于暂存更新操作
    private static class ProductStoreStatusUpdate {
        Long productId;
        Long shopId;

        public ProductStoreStatusUpdate(Long productId, Long shopId) {
            this.productId = productId;
            this.shopId = shopId;
        }
    }
}
