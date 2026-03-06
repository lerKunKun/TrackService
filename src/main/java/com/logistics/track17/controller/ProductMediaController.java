package com.logistics.track17.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.logistics.track17.dto.ProductMediaDTO;
import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.Product;
import com.logistics.track17.entity.ProductMedia;
import com.logistics.track17.mapper.ProductMapper;
import com.logistics.track17.service.MinioService;
import com.logistics.track17.service.ProductMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/product-media")
@RequiredArgsConstructor
public class ProductMediaController {

    private final ProductMediaService productMediaService;
    private final MinioService minioService;
    private final ProductMapper productMapper;

    @Value("${minio.product-media-bucket:product-media}")
    private String bucket;

    /**
     * 分页获取产品列表（带各 tag 文件统计）
     */
    @GetMapping("/list")
    public Result<Page<ProductMediaDTO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title) {

        minioService.ensureBucketExists(bucket);

        Page<Product> productPage = new Page<>(current, size);
        QueryWrapper<Product> qw = new QueryWrapper<Product>().orderByDesc("id");
        if (title != null && !title.isBlank()) {
            qw.like("title", title);
        }
        productMapper.selectPage(productPage, qw);

        Page<ProductMediaDTO> result = new Page<>(current, size, productPage.getTotal());
        List<ProductMediaDTO> dtos = productPage.getRecords().stream().map(product -> {
            ProductMediaDTO dto = new ProductMediaDTO();
            dto.setProductId(product.getId());
            dto.setProductName(product.getTitle());
            dto.setDescription(product.getBodyHtml());

            ProductMedia media = productMediaService.getOne(
                    new QueryWrapper<ProductMedia>().eq("product_id", product.getId()));
            if (media != null) {
                dto.setId(media.getId());
                dto.setReferenceLink(media.getReferenceLink());
            }

            String prefix = product.getId() + "/";
            try {
                List<MinioService.MinioFileInfo> files = minioService.listFiles(bucket, prefix);
                Map<String, Long> counts = new HashMap<>();
                counts.put("main-image", files.stream().filter(f -> "main-image".equals(f.tag)).count());
                counts.put("detail-media", files.stream().filter(f -> "detail-media".equals(f.tag)).count());
                counts.put("ad-media", files.stream().filter(f -> "ad-media".equals(f.tag)).count());
                dto.setTagFileCounts(counts);
            } catch (Exception e) {
                log.warn("Failed to count files for product {}", product.getId());
            }
            return dto;
        }).toList();

        result.setRecords(dtos);
        return Result.success(result);
    }

    /**
     * 获取某产品某 tag 的文件列表
     */
    @GetMapping("/{productId}/files")
    public Result<List<MinioService.MinioFileInfo>> listFiles(
            @PathVariable Long productId,
            @RequestParam(required = false) String tag) {
        String prefix = tag != null && !tag.isBlank()
                ? productId + "/" + tag + "/"
                : productId + "/";
        return Result.success(minioService.listFiles(bucket, prefix));
    }

    /**
     * 上传文件到 MinIO
     */
    @PostMapping("/{productId}/upload")
    public Result<MinioService.MinioFileInfo> upload(
            @PathVariable Long productId,
            @RequestParam String tag,
            @RequestParam("file") MultipartFile file) {

        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : "";
        String objectName = productId + "/" + tag + "/" + UUID.randomUUID() + ext;
        minioService.uploadFile(bucket, objectName, file);

        String url = minioService.getDirectUrl(bucket, objectName);
        String mediaType = file.getContentType() != null && file.getContentType().startsWith("video")
                ? "video"
                : "image";
        return Result.success(new MinioService.MinioFileInfo(
                objectName, originalName != null ? originalName : objectName,
                tag, mediaType, file.getSize(), url));
    }

    /**
     * 批量从 URL 下载媒体文件并保存到 MinIO
     * Body: { "tag": "main-image", "urls": ["http://...", ...] }
     */
    @PostMapping("/{productId}/download-urls")
    public Result<List<Map<String, Object>>> downloadFromUrls(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> body) {

        String tag = (String) body.get("tag");
        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) body.get("urls");

        if (tag == null || urls == null || urls.isEmpty()) {
            return Result.error("参数不能为空");
        }

        List<Map<String, Object>> results = new ArrayList<>();

        for (String rawUrl : urls) {
            String urlStr = rawUrl.trim();
            if (urlStr.isBlank())
                continue;

            Map<String, Object> item = new HashMap<>();
            item.put("url", urlStr);

            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.connect();

                int code = conn.getResponseCode();
                if (code != 200) {
                    item.put("success", false);
                    item.put("error", "HTTP " + code);
                    results.add(item);
                    continue;
                }

                // 确定文件名和扩展名
                String contentType = conn.getContentType();
                String ext = guessExtFromContentType(contentType, urlStr);
                String fileName = UUID.randomUUID() + ext;
                String objectName = productId + "/" + tag + "/" + fileName;

                long contentLength = conn.getContentLengthLong();

                try (InputStream is = conn.getInputStream()) {
                    minioService.uploadStream(bucket, objectName, is,
                            contentLength > 0 ? contentLength : -1,
                            contentType != null ? contentType : "application/octet-stream");
                }

                String directUrl = minioService.getDirectUrl(bucket, objectName);
                String mediaType = contentType != null && contentType.startsWith("video") ? "video" : "image";

                item.put("success", true);
                item.put("objectName", objectName);
                item.put("mediaType", mediaType);
                item.put("downloadUrl", directUrl);
                item.put("fileName", fileName);

            } catch (Exception e) {
                log.error("Failed to download URL: {}", urlStr, e);
                item.put("success", false);
                item.put("error", e.getMessage());
            }
            results.add(item);
        }

        return Result.success(results);
    }

    /**
     * 静默拉取产品主图到 MinIO（{productId}/main-image/）
     * 来源：product_images.src + product_variants.image_url，URL MD5 作文件名硬去重
     */
    @PostMapping("/{productId}/sync-images")
    public Result<Map<String, Object>> syncImages(@PathVariable Long productId) {
        // 查询该产品下所有图片 URL（product_images + product_variants，去重）
        List<String> imageUrls;
        try {
            imageUrls = productMapper.selectVariantImageUrls(productId);
        } catch (Exception e) {
            log.error("Failed to query variant image urls for product {}", productId, e);
            return Result.error("查询变体图片失败: " + e.getMessage());
        }

        if (imageUrls == null || imageUrls.isEmpty()) {
            return Result.success(
                    Map.of("synced", 0, "skipped", 0, "failed", 0, "message", "该产品暂无图片数据（product_images 与变体均为空）"));
        }

        // 查询 MinIO 中已有的文件（避免重复下载）
        String prefix = productId + "/main-image/";
        Set<String> existingFileNames;
        try {
            existingFileNames = minioService.listFiles(bucket, prefix).stream()
                    .map(f -> f.fileName)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            existingFileNames = new HashSet<>();
        }

        int synced = 0, skipped = 0, failed = 0;

        for (String rawUrl : imageUrls) {
            if (rawUrl == null || rawUrl.isBlank()) {
                skipped++;
                continue;
            }
            String urlStr = rawUrl.trim();

            // 用 URL MD5 作文件名，确保幂等性
            String urlHash = toMd5(urlStr);
            String ext = guessExtFromContentType(null, urlStr);
            String fileName = urlHash + ext;

            if (existingFileNames.contains(fileName)) {
                skipped++;
                continue;
            }

            String objectName = productId + "/main-image/" + fileName;
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.connect();

                if (conn.getResponseCode() != 200) {
                    log.warn("Sync image failed (HTTP {}): {}", conn.getResponseCode(), urlStr);
                    failed++;
                    continue;
                }

                String contentType = conn.getContentType();
                long contentLength = conn.getContentLengthLong();

                // 若通过 content-type 拿到更准确的扩展名，重建对象路径
                String betterExt = guessExtFromContentType(contentType, urlStr);
                if (!betterExt.equals(ext)) {
                    fileName = urlHash + betterExt;
                    objectName = productId + "/main-image/" + fileName;
                }

                try (InputStream is = conn.getInputStream()) {
                    minioService.uploadStream(bucket, objectName, is,
                            contentLength > 0 ? contentLength : -1,
                            contentType != null ? contentType : "image/jpeg");
                }
                existingFileNames.add(fileName);
                synced++;
                log.info("Synced image for product {}: {}", productId, objectName);

            } catch (Exception e) {
                log.error("Failed to sync image: {}", urlStr, e);
                failed++;
            }
        }

        return Result.success(Map.of(
                "synced", synced,
                "skipped", skipped,
                "failed", failed,
                "message", String.format("同步完成：%d 个新增，%d 个已存在跳过，%d 个失败", synced, skipped, failed)));
    }

    private static String toMd5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }

    /**
     * 删除文件
     */

    @DeleteMapping("/file")
    public Result<Boolean> deleteFile(@RequestParam String objectName) {
        minioService.deleteFile(bucket, objectName);
        return Result.success(true);
    }

    /**
     * 获取对标页链接列表
     */
    @GetMapping("/{productId}/reference-link")
    public Result<List<String>> getReferenceLink(@PathVariable Long productId) {
        ProductMedia media = productMediaService.getOne(
                new QueryWrapper<ProductMedia>().eq("product_id", productId));
        List<String> links = media != null ? media.getReferenceLink() : Collections.emptyList();
        return Result.success(links != null ? links : Collections.emptyList());
    }

    /**
     * 更新对标页链接列表
     */
    @PutMapping("/{productId}/reference-link")
    public Result<Boolean> updateReferenceLink(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> links = (List<String>) body.get("referenceLinks");
        ProductMedia media = productMediaService.getOne(
                new QueryWrapper<ProductMedia>().eq("product_id", productId));
        if (media == null) {
            media = new ProductMedia();
            media.setProductId(productId);
        }
        media.setReferenceLink(links != null ? links : Collections.emptyList());
        productMediaService.saveOrUpdate(media);
        return Result.success(true);
    }

    private String guessExtFromContentType(String contentType, String url) {
        if (contentType != null) {
            if (contentType.contains("jpeg") || contentType.contains("jpg"))
                return ".jpg";
            if (contentType.contains("png"))
                return ".png";
            if (contentType.contains("gif"))
                return ".gif";
            if (contentType.contains("webp"))
                return ".webp";
            if (contentType.contains("mp4"))
                return ".mp4";
            if (contentType.contains("mov"))
                return ".mov";
            if (contentType.contains("avi"))
                return ".avi";
            if (contentType.contains("webm"))
                return ".webm";
        }
        // 从 URL path 猜
        String path = url.split("\\?")[0];
        int dot = path.lastIndexOf('.');
        if (dot > 0 && dot > path.lastIndexOf('/')) {
            String ext = path.substring(dot).toLowerCase();
            if (ext.length() <= 5)
                return ext;
        }
        return ".jpg";
    }
}
