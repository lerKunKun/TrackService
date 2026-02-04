package com.logistics.track17.service;

import com.logistics.track17.dto.ProductDTO;
import com.logistics.track17.dto.ProductProcurementSummaryDTO;
import com.logistics.track17.dto.ProductSearchRequest;
import com.logistics.track17.entity.Product;
import com.logistics.track17.entity.ProductImport;
import com.logistics.track17.entity.ProductVariant;
import com.logistics.track17.mapper.ProductMapper;
import com.logistics.track17.mapper.ProductShopMapper;
import com.logistics.track17.mapper.ProductVariantMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 产品业务逻辑层
 * 处理 Shopify CSV 导入和产品管理
 */
@Slf4j
@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductShopMapper productShopMapper;

    @Autowired
    private ProductVariantMapper productVariantMapper;

    @Value("${storage.local.base-path:./storage/product-imports}")
    private String storagePath;

    /**
     * 导入 Shopify 产品 CSV
     * 
     * @param file CSV 文件
     * @return 导入记录对象
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductImport importShopifyCsv(MultipartFile file) throws IOException {
        log.info("开始导入 Shopify CSV 文件: {}", file.getOriginalFilename());

        // 1. 保存文件
        String fileName = file.getOriginalFilename();
        Path uploadPath = Paths.get(storagePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String savedFileName = System.currentTimeMillis() + "_" + fileName;
        Path filePath = uploadPath.resolve(savedFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 2. 创建导入记录
        ProductImport importRecord = new ProductImport();
        importRecord.setFileName(fileName);
        importRecord.setFilePath(filePath.toString());
        importRecord.setStatus(0); // 进行中
        importRecord.setCreatedAt(LocalDateTime.now());

        try {
            // 3. 解析 CSV
            List<Product> products = new ArrayList<>();
            Map<String, List<ProductVariant>> variantsMap = new HashMap<>();

            parseShopifyCsv(file, products, variantsMap);

            // 4. 保存产品和变体
            int successCount = 0;
            for (Product product : products) {
                // 检查 handle 是否已存在
                Product existing = productMapper.selectByHandle(product.getHandle());
                if (existing != null) {
                    log.warn("产品 handle 已存在，跳过: {}", product.getHandle());
                    continue;
                }

                // 插入产品
                productMapper.insert(product);

                // 插入变体
                List<ProductVariant> variants = variantsMap.get(product.getHandle());
                if (variants != null && !variants.isEmpty()) {
                    for (ProductVariant variant : variants) {
                        variant.setProductId(product.getId());
                    }
                    productVariantMapper.batchInsert(variants);
                }

                successCount++;
            }

            // 5. 更新导入记录
            importRecord.setStatus(1); // 成功
            importRecord.setTotalRecords(products.size());
            importRecord.setSuccessRecords(successCount);
            importRecord.setCompletedAt(LocalDateTime.now());

            log.info("CSV 导入完成，总数: {}, 成功: {}", products.size(), successCount);

        } catch (Exception e) {
            log.error("CSV 导入失败", e);
            importRecord.setStatus(2); // 失败
            importRecord.setErrorMessage(e.getMessage());
            importRecord.setCompletedAt(LocalDateTime.now());
            throw e;
        }

        return importRecord;
    }

    /**
     * 解析 Shopify CSV 文件
     * Shopify CSV 格式：第一行是产品信息+第一个变体，后续行是同一产品的其他变体
     */
    private void parseShopifyCsv(MultipartFile file, List<Product> products,
            Map<String, List<ProductVariant>> variantsMap) throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreHeaderCase(true)
                        .setTrim(true)
                        .build())) {

            String currentHandle = null;
            Product currentProduct = null;
            // 同一颜色的variants共享第一个variant的图片URL
            Map<String, String> colorImageMap = new HashMap<>();

            for (CSVRecord record : csvParser) {
                String handle = record.get("Handle");

                // 新产品
                if (!handle.equals(currentHandle)) {
                    // 创建新产品
                    currentHandle = handle;
                    currentProduct = new Product();
                    currentProduct.setHandle(handle);
                    currentProduct.setTitle(record.get("Title"));
                    currentProduct.setBodyHtml(getOptionalField(record, "Body (HTML)"));
                    currentProduct.setVendor(getOptionalField(record, "Vendor"));
                    currentProduct.setTags(getOptionalField(record, "Tags"));

                    // 解析 published 状态
                    String published = getOptionalField(record, "Published");
                    currentProduct.setPublished("TRUE".equalsIgnoreCase(published) ? 1 : 0);

                    products.add(currentProduct);
                    variantsMap.put(handle, new ArrayList<ProductVariant>());
                    // 新产品时清空颜色图片映射
                    colorImageMap.clear();
                }

                // 创建变体
                ProductVariant variant = new ProductVariant();
                variant.setTitle(getOptionalField(record, "Variant Title"));

                // 价格
                String price = getOptionalField(record, "Variant Price");
                if (price != null && !price.isEmpty()) {
                    variant.setPrice(new BigDecimal(price));
                }

                // 原价
                String compareAtPrice = getOptionalField(record, "Variant Compare At Price");
                if (compareAtPrice != null && !compareAtPrice.isEmpty()) {
                    variant.setCompareAtPrice(new BigDecimal(compareAtPrice));
                }

                // 图片处理：同一颜色的variants共享第一个variant的图片
                String option1Value = getOptionalField(record, "Option1 Value");
                String colorKey = (option1Value != null && !option1Value.isEmpty()) ? option1Value : "default";

                String variantImage = getOptionalField(record, "Variant Image");
                if (variantImage == null || variantImage.isEmpty()) {
                    variantImage = getOptionalField(record, "Image Src");
                }

                // 如果当前variant有图片URL，记录为该颜色的代表图片
                if (variantImage != null && !variantImage.isEmpty() && variantImage.startsWith("http")) {
                    colorImageMap.put(colorKey, variantImage);
                    variant.setImageUrl(variantImage);
                } else {
                    // 如果当前variant无图片，使用同颜色第一个variant的图片
                    String cachedImage = colorImageMap.get(colorKey);
                    variant.setImageUrl(cachedImage); // 如果该颜色还没有图片，则为null
                }

                // SKU
                variant.setSku(getOptionalField(record, "Variant SKU"));

                // 库存
                String inventory = getOptionalField(record, "Variant Inventory Qty");
                if (inventory != null && !inventory.isEmpty()) {
                    variant.setInventoryQuantity(Integer.parseInt(inventory));
                }

                // 重量
                String weight = getOptionalField(record, "Variant Grams");
                if (weight != null && !weight.isEmpty()) {
                    variant.setWeight(new BigDecimal(weight));
                }

                variant.setBarcode(getOptionalField(record, "Variant Barcode"));

                // 选项属性 (P2 字段，CSV中可能有)
                variant.setOption1Name(getOptionalField(record, "Option1 Name"));
                variant.setOption1Value(getOptionalField(record, "Option1 Value"));
                variant.setOption2Name(getOptionalField(record, "Option2 Name"));
                variant.setOption2Value(getOptionalField(record, "Option2 Value"));
                variant.setOption3Name(getOptionalField(record, "Option3 Name"));
                variant.setOption3Value(getOptionalField(record, "Option3 Value"));

                variantsMap.get(currentHandle).add(variant);
            }
        }
    }

    /**
     * 获取可选字段值
     */
    private String getOptionalField(CSVRecord record, String fieldName) {
        try {
            return record.isMapped(fieldName) ? record.get(fieldName) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 分页查询产品列表 (优化版: 解决N+1查询问题)
     */
    public Map<String, Object> searchProducts(ProductSearchRequest request) {
        // 1. 查询产品列表
        List<Product> products = productMapper.selectByPage(
                request.getTitle(),
                request.getTags(),
                request.getPublished(),
                request.getShopId(),
                request.getOffset(),
                request.getPageSize());

        // 2. 收集产品ID
        List<Long> productIds = new ArrayList<>();
        for (Product p : products) {
            productIds.add(p.getId());
        }

        // 3. 批量查询关联数据 (如果产品列表非空)
        Map<Long, List<Long>> productShopMap = new HashMap<>(); // productId -> List<shopId>
        Map<Long, ProductVariant> firstVariantMap = new HashMap<>(); // productId -> firstVariant
        Map<Long, Integer> variantCountMap = new HashMap<>(); // productId -> count

        if (!productIds.isEmpty()) {
            // A. 批量查询商店关联
            List<com.logistics.track17.entity.ProductShop> productShops = productShopMapper
                    .selectByProductIds(productIds);
            for (com.logistics.track17.entity.ProductShop ps : productShops) {
                productShopMap.computeIfAbsent(ps.getProductId(), k -> new ArrayList<>()).add(ps.getShopId());
            }

            // B. 批量查询第一个变体
            List<ProductVariant> firstVariants = productVariantMapper.selectFirstVariantsByProductIds(productIds);
            for (ProductVariant v : firstVariants) {
                firstVariantMap.put(v.getProductId(), v);
            }

            // C. 批量统计变体数量
            List<Map<String, Object>> counts = productVariantMapper.countVariantsByProductIds(productIds);
            for (Map<String, Object> map : counts) {
                Long pid = ((Number) map.get("product_id")).longValue();
                Integer count = ((Number) map.get("count")).intValue();
                variantCountMap.put(pid, count);
            }
        }

        // 4. 组装结果
        List<ProductDTO> productDTOs = new ArrayList<>();
        for (Product product : products) {
            ProductDTO dto = new ProductDTO();
            dto.setId(product.getId());
            dto.setHandle(product.getHandle());
            dto.setTitle(product.getTitle());
            dto.setVendor(product.getVendor());
            dto.setTags(product.getTags());
            dto.setPublished(product.getPublished());

            // 填充商店ID列表
            dto.setShopIds(productShopMap.getOrDefault(product.getId(), new ArrayList<>()));

            // 填充变体信息
            ProductVariant firstVariant = firstVariantMap.get(product.getId());
            if (firstVariant != null) {
                dto.setImageUrl(firstVariant.getImageUrl());
                dto.setPrice(firstVariant.getPrice());
                dto.setCompareAtPrice(firstVariant.getCompareAtPrice());
            }

            // 填充变体数量
            dto.setVariantCount(variantCountMap.getOrDefault(product.getId(), 0));

            productDTOs.add(dto);
        }

        // 5. 统计总数
        int total = productMapper.countByFilters(
                request.getTitle(),
                request.getTags(),
                request.getPublished(),
                request.getShopId());

        Map<String, Object> result = new HashMap<>();
        result.put("list", productDTOs);
        result.put("total", total);
        result.put("page", request.getPage());
        result.put("pageSize", request.getPageSize());

        return result;
    }

    /**
     * 转换为 DTO (单个转换，用于详情接口，保持兼容)
     */
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setHandle(product.getHandle());
        dto.setTitle(product.getTitle());
        dto.setVendor(product.getVendor());
        dto.setTags(product.getTags());
        dto.setPublished(product.getPublished());

        // 查询关联的商店ID列表
        List<Long> shopIds = productShopMapper.selectShopIdsByProductId(product.getId());
        dto.setShopIds(shopIds);

        // 查询第一个变体
        ProductVariant firstVariant = productVariantMapper.selectFirstByProductId(product.getId());
        if (firstVariant != null) {
            dto.setImageUrl(firstVariant.getImageUrl());
            dto.setPrice(firstVariant.getPrice());
            dto.setCompareAtPrice(firstVariant.getCompareAtPrice());
        }

        return dto;
    }

    /**
     * 根据ID查询产品
     */
    public Product getProductById(Long id) {
        return productMapper.selectById(id);
    }

    /**
     * 更新产品
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long id, Product product) {
        product.setId(id);
        productMapper.update(product);
    }

    /**
     * 更新产品商店关联
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProductShops(Long productId, List<Long> shopIds) {
        // 删除旧关联
        productShopMapper.deleteByProductId(productId);

        // 添加新关联
        if (shopIds != null && !shopIds.isEmpty()) {
            Long currentUserId = com.logistics.track17.util.UserContextHolder.getCurrentUserId();
            // Default to NOT_PUBLISHED (0) or similar, and use current user as
            // publisher/associator
            productShopMapper.batchInsert(productId, shopIds, currentUserId, 0);
        }
    }

    /**
     * 删除产品 (级联删除变体和商店关联)
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        // 删除商店关联
        productShopMapper.deleteByProductId(id);

        // 删除变体 (通过数据库外键级联删除)

        // 删除产品
        productMapper.deleteById(id);

        log.info("删除产品 ID: {}", id);
    }

    /**
     * 更新产品变体价格和原价
     * 
     * @param variantId      变体ID
     * @param price          销售价格
     * @param compareAtPrice 原价（对比价格）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateVariantPrice(Long variantId, BigDecimal price, BigDecimal compareAtPrice) {
        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setPrice(price);
        variant.setCompareAtPrice(compareAtPrice);

        productVariantMapper.updatePrice(variantId, price, compareAtPrice);

        log.info("更新变体价格 ID: {}, 价格: {}, 原价: {}", variantId, price, compareAtPrice);
    }

    /**
     * 批量更新产品所有变体的价格
     * 
     * @param productId      产品ID
     * @param price          销售价格
     * @param compareAtPrice 原价
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAllVariantsPrice(Long productId, BigDecimal price, BigDecimal compareAtPrice) {
        List<ProductVariant> variants = productVariantMapper.selectByProductId(productId);

        for (ProductVariant variant : variants) {
            productVariantMapper.updatePrice(variant.getId(), price, compareAtPrice);
        }

        log.info("批量更新产品变体价格 Product ID: {}, 变体数: {}", productId, variants.size());
    }

    /**
     * 查询产品的所有变体
     * 
     * @param productId 产品ID
     * @return 变体列表
     */
    public List<ProductVariant> getProductVariants(Long productId) {
        return productVariantMapper.selectByProductId(productId);
    }

    /**
     * 更新变体采购信息
     * 
     * @param variantId        变体ID
     * @param sku              SKU (可选)
     * @param procurementUrl   采购链接 (可选)
     * @param procurementPrice 采购价格 (可选)
     * @param supplier         采购商名称 (可选)
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateVariantProcurement(Long variantId, String sku,
            String procurementUrl, BigDecimal procurementPrice, String supplier) {
        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        if (sku != null) {
            variant.setSku(sku);
        }
        if (procurementUrl != null) {
            variant.setProcurementUrl(procurementUrl);
        }
        if (procurementPrice != null) {
            variant.setProcurementPrice(procurementPrice);
        }
        if (supplier != null) {
            variant.setSupplier(supplier);
        }
        productVariantMapper.update(variant);
        log.info("更新变体采购信息 ID: {}, SKU: {}, 采购商: {}", variantId, sku, supplier);
    }

    /**
     * 导出产品到Shopify CSV格式
     * 仅导出店铺产品SKU,不包含采购信息
     * 
     * @param shopId     店铺ID (可选,用于按店铺筛选)
     * @param productIds 产品ID列表 (可选,为空则导出全部)
     * @return CSV文本内容
     */
    public String exportProductsToCSV(Long shopId, List<Long> productIds) throws IOException {
        StringBuilder csv = new StringBuilder();

        // Shopify CSV 标准列头
        csv.append("Handle,Title,Body (HTML),Vendor,Tags,Published,");
        csv.append("Option1 Name,Option1 Value,Option2 Name,Option2 Value,Option3 Name,Option3 Value,");
        csv.append("Variant SKU,Variant Grams,Variant Inventory Tracker,Variant Inventory Qty,");
        csv.append("Variant Inventory Policy,Variant Fulfillment Service,Variant Price,Variant Compare At Price,");
        csv.append("Variant Requires Shipping,Variant Taxable,Variant Barcode,Image Src,");
        csv.append("Image Position,Image Alt Text,Gift Card,SEO Title,SEO Description,");
        csv.append("Google Shopping / Google Product Category,Google Shopping / Gender,Google Shopping / Age Group,");
        csv.append("Google Shopping / MPN,Google Shopping / AdWords Grouping,Google Shopping / AdWords Labels,");
        csv.append("Google Shopping / Condition,Google Shopping / Custom Product,Google Shopping / Custom Label 0,");
        csv.append(
                "Google Shopping / Custom Label 1,Google Shopping / Custom Label 2,Google Shopping / Custom Label 3,");
        csv.append("Google Shopping / Custom Label 4,Variant Image,Variant Weight Unit,Variant Tax Code,");
        csv.append("Cost per item,Status\n");

        // 查询产品列表
        List<Product> products;
        if (productIds != null && !productIds.isEmpty()) {
            // 按指定产品ID导出
            products = new ArrayList<>();
            for (Long id : productIds) {
                Product p = productMapper.selectById(id);
                if (p != null) {
                    products.add(p);
                }
            }
        } else {
            // 导出全部产品
            products = productMapper.selectByPage(null, null, null, shopId, 0, Integer.MAX_VALUE);
        }

        // 遍历产品
        for (Product product : products) {
            // 如果指定了shopId,检查产品是否关联该店铺
            if (shopId != null) {
                List<Long> shopIds = productShopMapper.selectShopIdsByProductId(product.getId());
                if (!shopIds.contains(shopId)) {
                    continue; // 跳过不属于该店铺的产品
                }
            }

            // 查询产品的所有变体
            List<ProductVariant> variants = productVariantMapper.selectByProductId(product.getId());

            if (variants.isEmpty()) {
                continue; // 跳过没有变体的产品
            }

            // 第一行: 产品信息 + 第一个变体
            ProductVariant firstVariant = variants.get(0);
            csv.append(escapeCsvField(product.getHandle())).append(",");
            csv.append(escapeCsvField(product.getTitle())).append(",");
            csv.append(escapeCsvField(product.getBodyHtml())).append(",");
            csv.append(escapeCsvField(product.getVendor())).append(",");
            csv.append(escapeCsvField(product.getTags())).append(",");
            csv.append(product.getPublished() == 1 ? "TRUE" : "FALSE").append(",");

            // 变体选项 - 处理必填检查
            String opt1Name = firstVariant.getOption1Name();
            String opt1Value = firstVariant.getOption1Value();

            if (opt1Name == null || opt1Name.isEmpty()) {
                opt1Name = "Title";
            }
            if (opt1Value == null || opt1Value.isEmpty()) {
                opt1Value = variants.size() == 1 ? "Default Title" : firstVariant.getTitle();
            }

            // 保存最终确定的Option Name，供后续变体验证使用
            final String finalOpt1Name = opt1Name;
            final String finalOpt2Name = firstVariant.getOption2Name();
            final String finalOpt3Name = firstVariant.getOption3Name();

            csv.append(escapeCsvField(finalOpt1Name)).append(",");
            csv.append(escapeCsvField(opt1Value)).append(",");
            csv.append(escapeCsvField(finalOpt2Name)).append(",");
            csv.append(escapeCsvField(firstVariant.getOption2Value())).append(",");
            csv.append(escapeCsvField(finalOpt3Name)).append(",");
            csv.append(escapeCsvField(firstVariant.getOption3Value())).append(",");

            // 变体信息 (仅包含店铺SKU,不包含采购信息)
            csv.append(escapeCsvField(firstVariant.getSku())).append(",");
            csv.append(firstVariant.getWeight() != null ? firstVariant.getWeight() : "").append(",");
            csv.append("shopify,"); // Inventory Tracker
            csv.append(firstVariant.getInventoryQuantity() != null ? firstVariant.getInventoryQuantity() : "0")
                    .append(",");
            csv.append("deny,"); // Inventory Policy
            csv.append("manual,"); // Fulfillment Service
            csv.append(firstVariant.getPrice() != null ? firstVariant.getPrice() : "").append(",");
            csv.append(firstVariant.getCompareAtPrice() != null ? firstVariant.getCompareAtPrice() : "").append(",");
            csv.append("TRUE,TRUE,"); // Requires Shipping, Taxable
            csv.append(escapeCsvField(firstVariant.getBarcode())).append(",");
            csv.append(escapeCsvField(firstVariant.getImageUrl())).append(",");
            csv.append("1,"); // Image Position
            csv.append(","); // Image Alt Text
            csv.append("FALSE,"); // Gift Card

            // SEO和Google Shopping字段 (暂时留空)
            for (int i = 0; i < 15; i++) {
                csv.append(",");
            }

            csv.append(escapeCsvField(firstVariant.getImageUrl())).append(","); // Variant Image
            csv.append("g,"); // Variant Weight Unit
            csv.append(","); // Variant Tax Code
            csv.append(","); // Cost per item (不导出采购价格)
            csv.append(product.getPublished() == 1 ? "active" : "draft");
            csv.append("\n");

            // 后续行: 其他变体 (仅变体信息,产品信息留空)
            for (int i = 1; i < variants.size(); i++) {
                ProductVariant variant = variants.get(i);

                // 产品信息列留空,仅填写Handle
                csv.append(escapeCsvField(product.getHandle())).append(",");
                csv.append(",,,,,"); // Title到Published留空

                // 变体选项 - 后续变体不需要重复Option Name，只填Value
                csv.append(","); // Option1 Name 留空

                String variantOpt1Value = matchOptionValue(variant, finalOpt1Name, 1);
                if (variantOpt1Value == null || variantOpt1Value.isEmpty()) {
                    // 尝试使用标题
                    variantOpt1Value = variant.getTitle();
                }
                // 如果标题也为空，且第一行定义了Option1 Name，必须填充默认值
                if ((variantOpt1Value == null || variantOpt1Value.isEmpty())
                        && (finalOpt1Name != null && !finalOpt1Name.isEmpty())) {
                    variantOpt1Value = "Default Value " + (i + 1);
                }
                csv.append(escapeCsvField(variantOpt1Value)).append(",");

                csv.append(","); // Option2 Name 留空
                String variantOpt2Value = matchOptionValue(variant, finalOpt2Name, 2);
                if ((variantOpt2Value == null || variantOpt2Value.isEmpty())
                        && (finalOpt2Name != null && !finalOpt2Name.isEmpty())) {
                    variantOpt2Value = "Default Value " + (i + 1);
                }
                csv.append(escapeCsvField(variantOpt2Value)).append(",");

                csv.append(","); // Option3 Name 留空
                String variantOpt3Value = matchOptionValue(variant, finalOpt3Name, 3);
                if ((variantOpt3Value == null || variantOpt3Value.isEmpty())
                        && (finalOpt3Name != null && !finalOpt3Name.isEmpty())) {
                    variantOpt3Value = "Default Value " + (i + 1);
                }
                csv.append(escapeCsvField(variantOpt3Value)).append(",");

                // 变体信息
                csv.append(escapeCsvField(variant.getSku())).append(",");
                csv.append(variant.getWeight() != null ? variant.getWeight() : "").append(",");
                csv.append("shopify,");
                csv.append(variant.getInventoryQuantity() != null ? variant.getInventoryQuantity() : "0").append(",");
                csv.append("deny,manual,");
                csv.append(variant.getPrice() != null ? variant.getPrice() : "").append(",");
                csv.append(variant.getCompareAtPrice() != null ? variant.getCompareAtPrice() : "").append(",");
                csv.append("TRUE,TRUE,");
                csv.append(escapeCsvField(variant.getBarcode())).append(",");
                csv.append(","); // Image Src留空（只有第一个variant有，后续variant通过Variant Image引用）
                csv.append((i + 1) + ",");

                // 其余字段留空
                for (int j = 0; j < 17; j++) {
                    csv.append(",");
                }

                String finalVariantImage = variant.getImageUrl();
                // 移除强制兜底逻辑，避免将主图(如粉色)强行赋予没有图片的变体(如裸色)
                // if (finalVariantImage == null || finalVariantImage.isEmpty()) {
                // finalVariantImage = firstVariant.getImageUrl();
                // }
                csv.append(escapeCsvField(finalVariantImage)).append(",");
                csv.append("g,,,");
                csv.append("\n");
            }
        }

        log.info("导出CSV完成,产品数: {}, 店铺ID: {}", products.size(), shopId);
        return csv.toString();
    }

    /**
     * 转义CSV字段 (处理逗号、换行符、引号)
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }

        // 如果包含逗号、换行符或引号,需要用引号包裹,并将引号转义为两个引号
        if (field.contains(",") || field.contains("\n") || field.contains("\"")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }

        return field;
    }

    /**
     * 根据 Option Name 匹配变体的 Option Value
     * 解决变体 Option 顺序与 Header 不一致的问题
     * 如果变体没有Option Name信息，则回退到按位置匹配
     */
    private String matchOptionValue(ProductVariant variant, String targetName, int defaultPosition) {
        if (targetName == null || targetName.isEmpty()) {
            return null;
        }

        // 1. 尝试按名称匹配 (Smart Mapping)
        if (targetName.equals(variant.getOption1Name())) {
            return variant.getOption1Value();
        }
        if (targetName.equals(variant.getOption2Name())) {
            return variant.getOption2Value();
        }
        if (targetName.equals(variant.getOption3Name())) {
            return variant.getOption3Value();
        }

        // 2. 如果没找到匹配，检查变体是否缺失 Option Name 信息
        // 如果该位置的 Option Name 为空，说明变体可能只是简单存储了 Value，此时回退到位置匹配
        String nameAtPos = null;
        if (defaultPosition == 1)
            nameAtPos = variant.getOption1Name();
        else if (defaultPosition == 2)
            nameAtPos = variant.getOption2Name();
        else if (defaultPosition == 3)
            nameAtPos = variant.getOption3Name();

        if (nameAtPos == null || nameAtPos.isEmpty()) {
            if (defaultPosition == 1)
                return variant.getOption1Value();
            else if (defaultPosition == 2)
                return variant.getOption2Value();
            else if (defaultPosition == 3)
                return variant.getOption3Value();
        }

        return null;
    }

    /**
     * 获取采购管理列表（分页+摘要信息）
     * 性能优化版本：使用JOIN查询，一次性获取产品和变体统计
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
     * @param status   筛选状态 (complete/incomplete/no-info)
     * @return 分页结果
     */
    public Map<String, Object> getProcurementList(Integer page, Integer pageSize, String keyword, String status) {
        // 计算偏移量
        int offset = (page - 1) * pageSize;

        // 构建查询参数
        Map<String, Object> params = new HashMap<>();
        params.put("offset", offset);
        params.put("pageSize", pageSize);
        params.put("keyword", keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null);
        params.put("status", status);

        // 查询数据
        List<ProductProcurementSummaryDTO> list = productMapper.selectProcurementList(params);
        long total = productMapper.countProcurementList(params);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (total + pageSize - 1) / pageSize);

        return result;
    }
}
