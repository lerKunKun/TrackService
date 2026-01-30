package com.logistics.track17.controller;

import com.logistics.track17.service.ProductListingService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 产品刊登控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/listing")
@CrossOrigin(origins = "*")
public class ProductListingController {

    private final ProductListingService productListingService;

    public ProductListingController(ProductListingService productListingService) {
        this.productListingService = productListingService;
    }

    /**
     * 导出Shopify CSV
     * 
     * @param request  导出请求
     * @param response HTTP响应
     */
    @PostMapping("/export/csv")
    public void exportCsv(@RequestBody ExportRequest request, HttpServletResponse response) {
        log.info("Received request to export CSV for {} products",
                request.getProductIds() != null ? request.getProductIds().size() : 0);

        try {
            byte[] csvData = productListingService.exportShopifyCsv(request.getProductIds());

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String fileName = "shopify_products_" + timestamp + ".csv";

            response.setContentType("text/csv; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            // 解决中文乱码 (BOM)
            response.getOutputStream().write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
            response.getOutputStream().write(csvData);
            response.flushBuffer();

        } catch (IOException e) {
            log.error("Failed to export CSV", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Data
    public static class ExportRequest {
        private List<Long> productIds;
    }
}
