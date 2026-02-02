package com.logistics.track17.controller;

import com.logistics.track17.service.ProductService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/listing")
public class ListingController {

    @Autowired
    private ProductService productService;

    /**
     * 导出产品到Shopify CSV格式
     */
    @PostMapping("/export/csv")
    public ResponseEntity<byte[]> exportShopifyCsv(@RequestBody ExportRequest request) {
        try {
            log.info("接收到CSV导出请求, 产品ID数量: {}", request.getProductIds() != null ? request.getProductIds().size() : 0);

            // 生成CSV内容
            String csvContent = productService.exportProductsToCSV(null, request.getProductIds());
            byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment",
                    "shopify_products_export_" + System.currentTimeMillis() + ".csv");
            headers.setContentLength(csvBytes.length);

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("导出CSV失败", e);
            // 返回错误信息
            String errorJson = "{\"success\":false,\"message\":\"导出失败: " + e.getMessage() + "\"}";
            byte[] errorBytes = errorJson.getBytes(StandardCharsets.UTF_8);
            return new ResponseEntity<>(errorBytes, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Data
    public static class ExportRequest {
        private List<Long> productIds;
    }
}
