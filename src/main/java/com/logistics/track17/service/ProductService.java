package com.logistics.track17.service;

import com.logistics.track17.dto.ProductDTO;
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

                // 图片
                variant.setImageUrl(getOptionalField(record, "Image Src"));

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
     * 分页查询产品列表
     */
    public Map<String, Object> searchProducts(ProductSearchRequest request) {
        // 查询产品列表
        List<Product> products = productMapper.selectByPage(
                request.getTitle(),
                request.getTags(),
                request.getPublished(),
                request.getShopId(),
                request.getOffset(),
                request.getPageSize());

        // 转换为 DTO
        List<ProductDTO> productDTOs = new ArrayList<>();
        for (Product product : products) {
            ProductDTO dto = convertToDTO(product);
            productDTOs.add(dto);
        }

        // 统计总数
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
     * 转换为 DTO (包含第一个变体和商店信息)
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
            productShopMapper.batchInsert(productId, shopIds);
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
}
