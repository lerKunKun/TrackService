package com.logistics.track17.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.track17.entity.Product;
import com.logistics.track17.entity.ProductMediaFile;
import com.logistics.track17.entity.Shop;
import com.logistics.track17.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 将产品数据 + 媒体文件推送到开发者店铺（Shopify REST API）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopifyProductPushService {

    private static final String API_VERSION = "2024-10";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 推送产品到开发者店铺，返回 Shopify 上创建的产品 ID。
     *
     * @param existingDevProductId 之前推送时记录的 ID，为 null 时新建
     * @return Shopify 上的产品 ID
     */
    public Long pushProduct(Shop devShop, Product product, List<ProductMediaFile> mediaFiles,
                            Long existingDevProductId) {
        Map<String, Object> productBody = buildProductPayload(product, mediaFiles);

        if (existingDevProductId != null) {
            return updateProduct(devShop, existingDevProductId, productBody);
        } else {
            return createProduct(devShop, productBody);
        }
    }

    private Long createProduct(Shop devShop, Map<String, Object> productBody) {
        String url = buildUrl(devShop, "/products.json");
        Map<String, Object> body = new HashMap<>();
        body.put("product", productBody);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders(devShop));

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            JsonNode created = objectMapper.readTree(response.getBody()).get("product");
            Long shopifyId = created.get("id").asLong();
            log.info("产品创建成功, devShop={}, shopifyProductId={}", devShop.getShopDomain(), shopifyId);
            return shopifyId;
        } catch (Exception e) {
            log.error("创建产品失败, devShop={}", devShop.getShopDomain(), e);
            throw BusinessException.of("推送产品到开发者店铺失败: " + e.getMessage());
        }
    }

    private Long updateProduct(Shop devShop, Long devProductId, Map<String, Object> productBody) {
        String url = buildUrl(devShop, "/products/" + devProductId + ".json");
        Map<String, Object> body = new HashMap<>();
        body.put("product", productBody);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders(devShop));

        try {
            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            log.info("产品更新成功, devShop={}, shopifyProductId={}", devShop.getShopDomain(), devProductId);
            return devProductId;
        } catch (Exception e) {
            log.warn("更新产品失败(可能已删除), 尝试重新创建, devShop={}", devShop.getShopDomain(), e);
            productBody.remove("id");
            return createProduct(devShop, productBody);
        }
    }

    private Map<String, Object> buildProductPayload(Product product, List<ProductMediaFile> mediaFiles) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", product.getTitle());
        payload.put("body_html", product.getBodyHtml());
        payload.put("vendor", product.getVendor());
        payload.put("tags", product.getTags());
        if (product.getHandle() != null) {
            payload.put("handle", product.getHandle());
        }

        List<Map<String, Object>> images = mediaFiles.stream()
                .filter(f -> "image".equals(f.getMediaType()))
                .sorted(Comparator.comparingInt(f -> f.getSortOrder() != null ? f.getSortOrder() : 0))
                .map(f -> {
                    Map<String, Object> img = new HashMap<>();
                    img.put("src", f.getUrl());
                    return img;
                })
                .collect(Collectors.toList());

        if (!images.isEmpty()) {
            payload.put("images", images);
        }

        return payload;
    }

    private HttpHeaders buildHeaders(Shop shop) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", shop.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String buildUrl(Shop shop, String path) {
        return String.format("https://%s/admin/api/%s%s", shop.getShopDomain(), API_VERSION, path);
    }
}
