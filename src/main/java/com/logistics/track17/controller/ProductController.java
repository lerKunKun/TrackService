package com.logistics.track17.controller;

import com.logistics.track17.annotation.AuditLog;
import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.dto.ProductSearchRequest;
import com.logistics.track17.entity.Product;
import com.logistics.track17.entity.ProductImport;
import com.logistics.track17.entity.ProductVariant;
import com.logistics.track17.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品管理控制器
 * P1: 产品开发功能 (CRUD + CSV导入)
 */
@Slf4j
@RestController
@RequestMapping("/product")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 导入 Shopify 产品 CSV
     * 
     * @param file CSV 文件
     * @return 导入结果
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) {
        try {
            log.info("接收到 CSV 导入请求: {}", file.getOriginalFilename());

            // 验证文件类型
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(responseError("文件不能为空"));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
                return ResponseEntity.badRequest().body(responseError("只支持CSV文件"));
            }

            // 执行导入
            ProductImport importRecord = productService.importShopifyCsv(file);

            // 构建导入结果数据 (Java 8兼容方式)
            Map<String, Object> importData = new HashMap<>();
            importData.put("totalRecords", importRecord.getTotalRecords());
            importData.put("successRecords", importRecord.getSuccessRecords());
            importData.put("fileName", importRecord.getFileName());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "导入成功");
            result.put("data", importData);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("CSV 导入失败", e);
            return ResponseEntity.status(500).body(responseError("导入失败: " + e.getMessage()));
        }
    }

    /**
     * 分页查询产品列表
     * 
     * @param request 搜索请求
     * @return 产品列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getProductList(ProductSearchRequest request) {
        try {
            Map<String, Object> result = productService.searchProducts(request);
            return ResponseEntity.ok(responseSuccess(result));
        } catch (Exception e) {
            log.error("查询产品列表失败", e);
            return ResponseEntity.status(500).body(responseError("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID查询产品详情
     * 
     * @param id 产品ID
     * @return 产品详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id);
            if (product == null) {
                return ResponseEntity.status(404).body(responseError("产品不存在"));
            }
            return ResponseEntity.ok(responseSuccess(product));
        } catch (Exception e) {
            log.error("查询产品详情失败", e);
            return ResponseEntity.status(500).body(responseError("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 更新产品
     * 
     * @param id            产品ID
     * @param updateRequest 更新请求
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductUpdateRequest updateRequest) {
        try {
            // 更新产品基本信息
            Product product = new Product();
            product.setHandle(updateRequest.getHandle());
            product.setTitle(updateRequest.getTitle());
            product.setVendor(updateRequest.getVendor());
            product.setTags(updateRequest.getTags());
            product.setPublished(updateRequest.getPublished());

            productService.updateProduct(id, product);

            // 更新商店关联
            if (updateRequest.getShopIds() != null) {
                productService.updateProductShops(id, updateRequest.getShopIds());
            }

            return ResponseEntity.ok(responseSuccess("更新成功"));
        } catch (Exception e) {
            log.error("更新产品失败", e);
            return ResponseEntity.status(500).body(responseError("更新失败: " + e.getMessage()));
        }
    }

    /**
     * 删除产品
     * 
     * @param id 产品ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(responseSuccess("删除成功"));
        } catch (Exception e) {
            log.error("删除产品失败", e);
            return ResponseEntity.status(500).body(responseError("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 构造成功响应
     */
    private Map<String, Object> responseSuccess(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    /**
     * 构造错误响应
     */
    private Map<String, Object> responseError(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }

    /**
     * 产品更新请求DTO
     */
    @lombok.Data
    public static class ProductUpdateRequest {
        private String handle;
        private String title;
        private String vendor;
        private String tags;
        private Integer published;
        private List<Long> shopIds;
    }

    /**
     * 获取产品的所有变体
     * 
     * @param productId 产品ID
     * @return 变体列表
     */
    @GetMapping("/{productId}/variants")
    public ResponseEntity<Map<String, Object>> getProductVariants(@PathVariable Long productId) {
        try {
            List<ProductVariant> variants = productService.getProductVariants(productId);
            return ResponseEntity.ok(responseSuccess(variants));
        } catch (Exception e) {
            log.error("查询产品变体失败", e);
            return ResponseEntity.status(500).body(responseError("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 更新单个变体的价格和原价
     * 
     * @param variantId 变体ID
     * @param request   价格更新请求
     * @return 更新结果
     */
    @PutMapping("/variants/{variantId}/price")
    @RequireAuth(permissions = "product:price:update")
    @AuditLog(operation = "修改产品价格", module = "产品管理")
    public ResponseEntity<Map<String, Object>> updateVariantPrice(
            @PathVariable Long variantId,
            @RequestBody PriceUpdateRequest request) {
        try {
            productService.updateVariantPrice(variantId, request.getPrice(), request.getCompareAtPrice());
            return ResponseEntity.ok(responseSuccess("价格更新成功"));
        } catch (Exception e) {
            log.error("更新变体价格失败", e);
            return ResponseEntity.status(500).body(responseError("更新失败: " + e.getMessage()));
        }
    }

    /**
     * 批量更新产品所有变体的价格
     * 
     * @param productId 产品ID
     * @param request   价格更新请求
     * @return 更新结果
     */
    @PutMapping("/{productId}/price")
    @RequireAuth(permissions = "product:price:update")
    @AuditLog(operation = "批量修改产品价格", module = "产品管理")
    public ResponseEntity<Map<String, Object>> updateAllVariantsPrice(
            @PathVariable Long productId,
            @RequestBody PriceUpdateRequest request) {
        try {
            productService.updateAllVariantsPrice(productId, request.getPrice(), request.getCompareAtPrice());
            return ResponseEntity.ok(responseSuccess("批量价格更新成功"));
        } catch (Exception e) {
            log.error("批量更新变体价格失败", e);
            return ResponseEntity.status(500).body(responseError("更新失败: " + e.getMessage()));
        }
    }

    /**
     * 价格更新请求DTO
     */
    @lombok.Data
    public static class PriceUpdateRequest {
        private BigDecimal price; // 销售价格
        private BigDecimal compareAtPrice; // 原价（划线价）
    }

    /**
     * 更新变体采购信息
     * 可以更新SKU、采购链接、采购价格、供应商等信息
     * 
     * @param variantId 变体ID
     * @param request   采购信息更新请求
     * @return 更新结果
     */
    @PutMapping("/variants/{variantId}/procurement")
    public ResponseEntity<Map<String, Object>> updateVariantProcurement(
            @PathVariable Long variantId,
            @RequestBody VariantProcurementUpdateRequest request) {
        try {
            productService.updateVariantProcurement(
                    variantId,
                    request.getSku(),
                    request.getProcurementUrl(),
                    request.getProcurementPrice(),
                    request.getSupplier());
            return ResponseEntity.ok(responseSuccess("变体采购信息更新成功"));
        } catch (Exception e) {
            log.error("更新变体采购信息失败", e);
            return ResponseEntity.status(500).body(responseError("更新失败: " + e.getMessage()));
        }
    }

    /**
     * 导出产品到Shopify CSV格式
     * 
     * @param shopId     店铺ID (可选,用于按店铺筛选)
     * @param productIds 产品ID列表 (可选,逗号分隔)
     * @return CSV文件下载
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportProductsCSV(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String productIds) {
        try {
            // 解析产品ID列表
            List<Long> ids = null;
            if (productIds != null && !productIds.trim().isEmpty()) {
                ids = new ArrayList<>();
                for (String id : productIds.split(",")) {
                    try {
                        ids.add(Long.parseLong(id.trim()));
                    } catch (NumberFormatException e) {
                        log.warn("无效的产品ID: {}", id);
                    }
                }
            }

            // 生成CSV内容
            String csvContent = productService.exportProductsToCSV(shopId, ids);
            byte[] csvBytes = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // 设置响应头
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment",
                    "products_export_" + System.currentTimeMillis() + ".csv");
            headers.setContentLength(csvBytes.length);

            return new ResponseEntity<>(csvBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            log.error("导出CSV失败", e);
            // 返回错误信息(JSON格式)
            String errorJson = "{\"success\":false,\"message\":\"导出失败: " + e.getMessage() + "\"}";
            byte[] errorBytes = errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return new ResponseEntity<>(errorBytes, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 变体采购信息更新请求DTO
     */
    @lombok.Data
    public static class VariantProcurementUpdateRequest {
        private String sku;
        private String procurementUrl;
        private BigDecimal procurementPrice;
        private String supplier;
    }
}
