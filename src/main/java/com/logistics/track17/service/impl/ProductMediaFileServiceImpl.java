package com.logistics.track17.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.logistics.track17.entity.ProductMediaFile;
import com.logistics.track17.mapper.ProductMapper;
import com.logistics.track17.mapper.ProductMediaFileMapper;
import com.logistics.track17.service.MinioService;
import com.logistics.track17.service.ProductMediaFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMediaFileServiceImpl
        extends ServiceImpl<ProductMediaFileMapper, ProductMediaFile>
        implements ProductMediaFileService {

    private final MinioService minioService;
    private final ProductMapper productMapper;

    @Value("${minio.product-media-bucket:product-media}")
    private String bucket;

    @Override
    public List<ProductMediaFile> listByProductAndCategory(Long productId, String category) {
        return list(new QueryWrapper<ProductMediaFile>()
                .eq("product_id", productId)
                .eq("category", category)
                .orderByAsc("sort_order", "id"));
    }

    @Override
    public List<ProductMediaFile> listByProduct(Long productId) {
        return list(new QueryWrapper<ProductMediaFile>()
                .eq("product_id", productId)
                .orderByAsc("category", "sort_order", "id"));
    }

    @Override
    public Map<Long, Map<String, Long>> countByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = baseMapper.countByProductIds(productIds);
        Map<Long, Map<String, Long>> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long pid = ((Number) row.get("product_id")).longValue();
            String cat = (String) row.get("category");
            Long cnt = ((Number) row.get("cnt")).longValue();
            result.computeIfAbsent(pid, k -> new HashMap<>()).put(cat, cnt);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductMediaFile uploadFile(Long productId, String category, MultipartFile file, Long uploaderId) {
        minioService.ensureBucketExists(bucket);

        String originalName = file.getOriginalFilename();
        String ext = extractExtension(originalName);
        String objectName = productId + "/" + category + "/" + UUID.randomUUID() + ext;

        minioService.uploadFile(bucket, objectName, file);

        String url = minioService.getDirectUrl(bucket, objectName);
        String mediaType = detectMediaType(file.getContentType(), originalName);
        int nextSort = baseMapper.getMaxSortOrder(productId, category) + 1;

        ProductMediaFile record = new ProductMediaFile();
        record.setProductId(productId);
        record.setCategory(category);
        record.setOriginalName(originalName != null ? originalName : objectName);
        record.setObjectName(objectName);
        record.setContentType(file.getContentType());
        record.setFileSize(file.getSize());
        record.setMediaType(mediaType);
        record.setUrl(url);
        record.setSource("UPLOAD");
        record.setSortOrder(nextSort);
        record.setUploaderId(uploaderId);
        save(record);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductMediaFile downloadFromUrl(Long productId, String category, String rawUrl, Long uploaderId) {
        minioService.ensureBucketExists(bucket);

        URL url;
        try {
            url = new URL(rawUrl.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的URL: " + rawUrl);
        }

        if (!isAllowedUrl(url)) {
            throw new IllegalArgumentException("不允许的URL：仅支持 http/https 公网地址");
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.connect();

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new RuntimeException("HTTP " + code);
            }

            String contentType = conn.getContentType();
            String ext = guessExtFromContentType(contentType, rawUrl);
            String fileName = UUID.randomUUID() + ext;
            String objectName = productId + "/" + category + "/" + fileName;
            long contentLength = conn.getContentLengthLong();

            try (InputStream is = conn.getInputStream()) {
                minioService.uploadStream(bucket, objectName, is,
                        contentLength > 0 ? contentLength : -1,
                        contentType != null ? contentType : "application/octet-stream");
            }

            String directUrl = minioService.getDirectUrl(bucket, objectName);
            String mediaType = detectMediaType(contentType, fileName);
            int nextSort = baseMapper.getMaxSortOrder(productId, category) + 1;

            ProductMediaFile record = new ProductMediaFile();
            record.setProductId(productId);
            record.setCategory(category);
            record.setOriginalName(extractFileNameFromUrl(rawUrl, fileName));
            record.setObjectName(objectName);
            record.setContentType(contentType);
            record.setFileSize(contentLength > 0 ? contentLength : 0);
            record.setMediaType(mediaType);
            record.setUrl(directUrl);
            record.setSource("URL_DOWNLOAD");
            record.setSourceUrl(rawUrl);
            record.setSortOrder(nextSort);
            record.setUploaderId(uploaderId);
            save(record);

            return record;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId) {
        ProductMediaFile file = getById(fileId);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        try {
            minioService.deleteFile(bucket, file.getObjectName());
        } catch (Exception e) {
            log.warn("MinIO 删除失败，继续删除 DB 记录: {}", file.getObjectName(), e);
        }
        removeById(fileId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteFiles(Long productId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) return;

        List<ProductMediaFile> files = list(new QueryWrapper<ProductMediaFile>()
                .in("id", fileIds)
                .eq("product_id", productId));

        for (ProductMediaFile file : files) {
            try {
                minioService.deleteFile(bucket, file.getObjectName());
            } catch (Exception e) {
                log.warn("MinIO 批量删除失败: {}", file.getObjectName(), e);
            }
        }
        removeByIds(files.stream().map(ProductMediaFile::getId).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSort(Long productId, String category, List<Long> sortedIds) {
        for (int i = 0; i < sortedIds.size(); i++) {
            ProductMediaFile update = new ProductMediaFile();
            update.setId(sortedIds.get(i));
            update.setSortOrder(i);
            updateById(update);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveCategory(Long fileId, String newCategory) {
        ProductMediaFile file = getById(fileId);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在");
        }

        String oldObjectName = file.getObjectName();
        String newObjectName = file.getProductId() + "/" + newCategory + "/"
                + oldObjectName.substring(oldObjectName.lastIndexOf('/') + 1);

        try {
            // MinIO copy + delete (move)
            minioService.copyObject(bucket, oldObjectName, bucket, newObjectName);
            minioService.deleteFile(bucket, oldObjectName);
        } catch (Exception e) {
            throw new RuntimeException("移动文件失败: " + e.getMessage(), e);
        }

        int nextSort = baseMapper.getMaxSortOrder(file.getProductId(), newCategory) + 1;
        file.setCategory(newCategory);
        file.setObjectName(newObjectName);
        file.setUrl(minioService.getDirectUrl(bucket, newObjectName));
        file.setSortOrder(nextSort);
        updateById(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncProductImages(Long productId, Long uploaderId) {
        minioService.ensureBucketExists(bucket);

        List<String> imageUrls;
        try {
            imageUrls = productMapper.selectVariantImageUrls(productId);
        } catch (Exception e) {
            log.error("查询变体图片失败: productId={}", productId, e);
            return Map.of("synced", 0, "skipped", 0, "failed", 0,
                    "message", "查询变体图片失败: " + e.getMessage());
        }

        if (imageUrls == null || imageUrls.isEmpty()) {
            return Map.of("synced", 0, "skipped", 0, "failed", 0,
                    "message", "该产品暂无图片数据");
        }

        Set<String> existingSourceUrls = list(new QueryWrapper<ProductMediaFile>()
                .eq("product_id", productId)
                .eq("category", "main_image")
                .isNotNull("source_url"))
                .stream()
                .map(ProductMediaFile::getSourceUrl)
                .collect(Collectors.toSet());

        int synced = 0, skipped = 0, failed = 0;

        for (String rawUrl : imageUrls) {
            if (rawUrl == null || rawUrl.isBlank()) { skipped++; continue; }
            String urlStr = rawUrl.trim();

            if (existingSourceUrls.contains(urlStr)) { skipped++; continue; }

            try {
                URL parsedUrl = new URL(urlStr);
                if (!isAllowedUrl(parsedUrl)) { failed++; continue; }

                HttpURLConnection conn = (HttpURLConnection) parsedUrl.openConnection();
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.connect();

                if (conn.getResponseCode() != 200) { failed++; continue; }

                String contentType = conn.getContentType();
                String ext = guessExtFromContentType(contentType, urlStr);
                String urlHash = toMd5(urlStr);
                String fileName = urlHash + ext;
                String objectName = productId + "/main_image/" + fileName;

                long contentLength = conn.getContentLengthLong();
                try (InputStream is = conn.getInputStream()) {
                    minioService.uploadStream(bucket, objectName, is,
                            contentLength > 0 ? contentLength : -1,
                            contentType != null ? contentType : "image/jpeg");
                }

                int nextSort = baseMapper.getMaxSortOrder(productId, "main_image") + 1;
                ProductMediaFile record = new ProductMediaFile();
                record.setProductId(productId);
                record.setCategory("main_image");
                record.setOriginalName(fileName);
                record.setObjectName(objectName);
                record.setContentType(contentType);
                record.setFileSize(contentLength > 0 ? contentLength : 0);
                record.setMediaType("image");
                record.setUrl(minioService.getDirectUrl(bucket, objectName));
                record.setSource("SHOPIFY_SYNC");
                record.setSourceUrl(urlStr);
                record.setSortOrder(nextSort);
                record.setUploaderId(uploaderId);
                save(record);

                existingSourceUrls.add(urlStr);
                synced++;
            } catch (Exception e) {
                log.error("同步图片失败: {}", urlStr, e);
                failed++;
            }
        }

        return Map.of(
                "synced", synced, "skipped", skipped, "failed", failed,
                "message", String.format("同步完成：%d 个新增，%d 个已存在跳过，%d 个失败",
                        synced, skipped, failed));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> migrateFromMinio() {
        minioService.ensureBucketExists(bucket);

        List<MinioService.MinioFileInfo> allFiles = minioService.listFiles(bucket, "");
        int migrated = 0, skipped = 0;

        for (MinioService.MinioFileInfo f : allFiles) {
            long existing = count(new QueryWrapper<ProductMediaFile>()
                    .eq("object_name", f.getObjectName()));
            if (existing > 0) { skipped++; continue; }

            String[] parts = f.getObjectName().split("/");
            if (parts.length < 3) { skipped++; continue; }

            Long productId;
            try {
                productId = Long.parseLong(parts[0]);
            } catch (NumberFormatException e) { skipped++; continue; }

            String category = normalizeCategory(parts[1]);

            ProductMediaFile record = new ProductMediaFile();
            record.setProductId(productId);
            record.setCategory(category);
            record.setOriginalName(f.getFileName());
            record.setObjectName(f.getObjectName());
            record.setMediaType(f.getMediaType());
            record.setFileSize(f.getSize());
            record.setUrl(f.getUrl());
            record.setSource("UPLOAD");
            record.setSortOrder(migrated);

            try {
                save(record);
                migrated++;
            } catch (Exception e) {
                log.warn("迁移文件记录失败: {}", f.getObjectName(), e);
                skipped++;
            }
        }

        return Map.of("migrated", migrated, "skipped", skipped,
                "message", String.format("迁移完成：%d 个写入，%d 个跳过", migrated, skipped));
    }

    // ─── Helper methods ───

    private String normalizeCategory(String tag) {
        return switch (tag) {
            case "main-image" -> "main_image";
            case "detail-media" -> "detail_media";
            case "ad-media" -> "ad_media";
            default -> tag.contains("-") ? tag.replace("-", "_") : tag;
        };
    }

    private String extractExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.'));
        }
        return "";
    }

    private String detectMediaType(String contentType, String fileName) {
        if (contentType != null) {
            if (contentType.startsWith("video")) return "video";
            if (contentType.startsWith("image")) return "image";
            if (contentType.contains("pdf") || contentType.contains("document")
                    || contentType.contains("spreadsheet") || contentType.contains("msword")) {
                return "document";
            }
        }
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov")
                    || lower.endsWith(".avi") || lower.endsWith(".mkv")) {
                return "video";
            }
            if (lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx")
                    || lower.endsWith(".xls") || lower.endsWith(".xlsx")) {
                return "document";
            }
        }
        return "image";
    }

    private String extractFileNameFromUrl(String urlStr, String fallback) {
        try {
            String path = urlStr.split("\\?")[0];
            int slash = path.lastIndexOf('/');
            if (slash >= 0 && slash < path.length() - 1) {
                String name = path.substring(slash + 1);
                if (name.length() <= 200) return name;
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    private boolean isAllowedUrl(URL url) {
        String protocol = url.getProtocol();
        if (!"http".equals(protocol) && !"https".equals(protocol)) return false;
        try {
            InetAddress address = InetAddress.getByName(url.getHost());
            return !(address.isLoopbackAddress() || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress() || address.isAnyLocalAddress());
        } catch (Exception e) {
            return false;
        }
    }

    private String guessExtFromContentType(String contentType, String url) {
        if (contentType != null) {
            if (contentType.contains("jpeg") || contentType.contains("jpg")) return ".jpg";
            if (contentType.contains("png")) return ".png";
            if (contentType.contains("gif")) return ".gif";
            if (contentType.contains("webp")) return ".webp";
            if (contentType.contains("mp4")) return ".mp4";
            if (contentType.contains("mov")) return ".mov";
            if (contentType.contains("avi")) return ".avi";
            if (contentType.contains("webm")) return ".webm";
            if (contentType.contains("pdf")) return ".pdf";
        }
        String path = url.split("\\?")[0];
        int dot = path.lastIndexOf('.');
        if (dot > 0 && dot > path.lastIndexOf('/')) {
            String ext = path.substring(dot).toLowerCase();
            if (ext.length() <= 5) return ext;
        }
        return ".jpg";
    }

    private static String toMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
