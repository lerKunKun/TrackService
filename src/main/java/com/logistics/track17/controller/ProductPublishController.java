package com.logistics.track17.controller;

import com.logistics.track17.dto.Result;
import com.logistics.track17.service.ProductPublishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Product Publish Controller
 * Handling product publishing operations
 */
@RestController
@RequestMapping("/product-publish")
public class ProductPublishController {

    @Autowired
    private ProductPublishService productPublishService;

    /**
     * Publish products to shops
     */
    @PostMapping("/publish")
    public Result<Object> publishProducts(@RequestBody Map<String, List<Long>> request) {
        List<Long> productIds = request.get("productIds");
        List<Long> shopIds = request.get("shopIds");

        if (productIds == null || productIds.isEmpty() || shopIds == null || shopIds.isEmpty()) {
            return Result.error(400, "Product IDs and Shop IDs are required");
        }

        int count = productPublishService.publishProducts(productIds, shopIds);
        return Result.success("Successfully published " + count + " products");
    }

    /**
     * Unpublish products from shops
     */
    @PostMapping("/unpublish")
    public Result<Object> unpublishProducts(@RequestBody Map<String, List<Long>> request) {
        List<Long> productIds = request.get("productIds");
        List<Long> shopIds = request.get("shopIds");

        if (productIds == null || productIds.isEmpty() || shopIds == null || shopIds.isEmpty()) {
            return Result.error(400, "Product IDs and Shop IDs are required");
        }

        productPublishService.unpublishProducts(productIds, shopIds);
        return Result.success("Successfully unpublished products");
    }
}
