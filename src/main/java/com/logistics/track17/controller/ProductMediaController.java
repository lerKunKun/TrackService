package com.logistics.track17.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.logistics.track17.dto.DownloadUrlsRequest;
import com.logistics.track17.dto.ProductMediaDTO;
import com.logistics.track17.dto.ReferenceLinkRequest;
import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.Product;
import com.logistics.track17.entity.ProductMedia;
import com.logistics.track17.entity.ProductMediaFile;
import com.logistics.track17.mapper.ProductMapper;
import com.logistics.track17.service.MinioService;
import com.logistics.track17.service.ProductMediaFileService;
import com.logistics.track17.service.ProductMediaService;
import com.logistics.track17.annotation.RequireAuth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/product-media")
@RequiredArgsConstructor
@RequireAuth
public class ProductMediaController {

    private final ProductMediaService productMediaService;
    private final ProductMediaFileService productMediaFileService;
    private final ProductMapper productMapper;
    private final MinioService minioService;

    @Value("${minio.product-media-bucket:product-media}")
    private String bucket;

    // ─── 产品列表（带文件统计） ───

    @GetMapping("/list")
    public Result<Page<ProductMediaDTO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title) {

        Page<Product> productPage = new Page<>(current, size);
        QueryWrapper<Product> qw = new QueryWrapper<Product>().orderByDesc("id");
        if (title != null && !title.isBlank()) {
            qw.like("title", title);
        }
        productMapper.selectPage(productPage, qw);

        List<Product> products = productPage.getRecords();
        if (products.isEmpty()) {
            Page<ProductMediaDTO> emptyResult = new Page<>(current, size, 0);
            emptyResult.setRecords(Collections.emptyList());
            return Result.success(emptyResult);
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();

        // DB batch count (replaces N+1 MinIO calls)
        Map<Long, Map<String, Long>> fileCounts = productMediaFileService.countByProductIds(productIds);

        List<ProductMedia> mediaList = productMediaService.list(
                new QueryWrapper<ProductMedia>().in("product_id", productIds));
        Map<Long, ProductMedia> mediaMap = mediaList.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProductMedia::getProductId, m -> m, (a, b) -> a));

        Page<ProductMediaDTO> result = new Page<>(current, size, productPage.getTotal());
        List<ProductMediaDTO> dtos = products.stream().map(product -> {
            ProductMediaDTO dto = new ProductMediaDTO();
            dto.setProductId(product.getId());
            dto.setProductName(product.getTitle());
            dto.setDescription(product.getBodyHtml());

            ProductMedia media = mediaMap.get(product.getId());
            if (media != null) {
                dto.setId(media.getId());
                dto.setReferenceLink(media.getReferenceLink());
            }

            Map<String, Long> counts = fileCounts.getOrDefault(product.getId(), Collections.emptyMap());
            dto.setTagFileCounts(counts);
            return dto;
        }).toList();

        result.setRecords(dtos);
        return Result.success(result);
    }

    // ─── 文件列表 ───

    @GetMapping("/{productId}/files")
    public Result<List<ProductMediaFile>> listFiles(
            @PathVariable Long productId,
            @RequestParam(required = false) String category) {
        List<ProductMediaFile> files;
        if (category != null && !category.isBlank()) {
            files = productMediaFileService.listByProductAndCategory(productId, category);
        } else {
            files = productMediaFileService.listByProduct(productId);
        }
        return Result.success(files);
    }

    // ─── 上传文件 ───

    @PostMapping("/{productId}/upload")
    public Result<ProductMediaFile> upload(
            @PathVariable Long productId,
            @RequestParam String category,
            @RequestParam(required = false) String tags,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long uploaderId = getUserId(request);
        ProductMediaFile saved = productMediaFileService.uploadFile(productId, category, tags, file, uploaderId);
        return Result.success(saved);
    }

    // ─── 批量 URL 下载 ───

    @PostMapping("/{productId}/download-urls")
    public Result<List<Map<String, Object>>> downloadFromUrls(
            @PathVariable Long productId,
            @RequestBody @javax.validation.Valid DownloadUrlsRequest request,
            HttpServletRequest httpRequest) {
        Long uploaderId = getUserId(httpRequest);
        String category = normalizeCategoryParam(request.getTag());
        List<String> urls = request.getUrls();
        List<Map<String, Object>> results = new ArrayList<>();

        for (String rawUrl : urls) {
            if (rawUrl == null || rawUrl.isBlank()) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("url", rawUrl.trim());
            try {
                ProductMediaFile saved = productMediaFileService.downloadFromUrl(
                        productId, category, request.getTags(), rawUrl, uploaderId);
                item.put("success", true);
                item.put("id", saved.getId());
                item.put("objectName", saved.getObjectName());
                item.put("mediaType", saved.getMediaType());
                item.put("downloadUrl", saved.getUrl());
                item.put("fileName", saved.getOriginalName());
            } catch (Exception e) {
                log.error("URL下载失败: {}", rawUrl, e);
                item.put("success", false);
                item.put("error", e.getMessage());
            }
            results.add(item);
        }
        return Result.success(results);
    }

    // ─── 同步 Shopify 主图 ───

    @PostMapping("/{productId}/sync-images")
    public Result<Map<String, Object>> syncImages(
            @PathVariable Long productId,
            HttpServletRequest request) {
        Long uploaderId = getUserId(request);
        return Result.success(productMediaFileService.syncProductImages(productId, uploaderId));
    }

    // ─── 删除单个文件 ───

    @DeleteMapping("/files/{fileId}")
    public Result<Boolean> deleteFile(@PathVariable Long fileId) {
        productMediaFileService.deleteFile(fileId);
        return Result.success(true);
    }

    // ─── 批量删除 ───

    @DeleteMapping("/{productId}/files/batch")
    public Result<Boolean> batchDelete(
            @PathVariable Long productId,
            @RequestBody List<Long> fileIds) {
        productMediaFileService.batchDeleteFiles(productId, fileIds);
        return Result.success(true);
    }

    // ─── 更新排序 ───

    @PutMapping("/{productId}/files/sort")
    public Result<Boolean> updateSort(
            @PathVariable Long productId,
            @RequestParam String category,
            @RequestBody List<Long> sortedIds) {
        productMediaFileService.updateSort(productId, category, sortedIds);
        return Result.success(true);
    }

    // ─── 移动分类 ───

    @PutMapping("/files/{fileId}/category")
    public Result<Boolean> moveCategory(
            @PathVariable Long fileId,
            @RequestParam String category) {
        productMediaFileService.moveCategory(fileId, category);
        return Result.success(true);
    }

    // ─── 对标链接 ───

    @GetMapping("/{productId}/reference-link")
    public Result<List<String>> getReferenceLink(@PathVariable Long productId) {
        ProductMedia media = productMediaService.getOne(
                new QueryWrapper<ProductMedia>().eq("product_id", productId));
        List<String> links = media != null ? media.getReferenceLink() : Collections.emptyList();
        return Result.success(links != null ? links : Collections.emptyList());
    }

    @PutMapping("/{productId}/reference-link")
    public Result<Boolean> updateReferenceLink(
            @PathVariable Long productId,
            @RequestBody ReferenceLinkRequest request) {
        List<String> links = request.getReferenceLinks();
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

    // ─── 数据迁移（一次性） ───

    @PostMapping("/migrate")
    @RequireAuth(permissions = "admin")
    public Result<Map<String, Object>> migrate() {
        return Result.success(productMediaFileService.migrateFromMinio());
    }

    // ─── 修复已有文件 URL（R2 endpoint -> CDN） ───

    @PostMapping("/fix-urls")
    @RequireAuth(permissions = "admin")
    public Result<Map<String, Object>> fixUrls() {
        return Result.success(productMediaFileService.fixUrls());
    }

    // ─── 单文件下载 ───

    @GetMapping("/files/{fileId}/download")
    public void downloadFile(@PathVariable Long fileId, HttpServletResponse response) {
        ProductMediaFile file = productMediaFileService.getById(fileId);
        if (file == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String fileName = file.getOriginalName() != null ? file.getOriginalName() : "download";
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        if (file.getFileSize() != null && file.getFileSize() > 0) {
            response.setContentLengthLong(file.getFileSize());
        }

        try (InputStream is = minioService.getObject(bucket, file.getObjectName());
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
            os.flush();
        } catch (Exception e) {
            log.error("文件下载失败: fileId={}", fileId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // ─── 批量下载（ZIP） ───

    @PostMapping("/files/batch-download")
    public void batchDownload(@RequestBody List<Long> fileIds, HttpServletResponse response) {
        if (fileIds == null || fileIds.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        List<ProductMediaFile> files = productMediaFileService.listByIds(fileIds);
        if (files.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String zipName = "media-files-" + System.currentTimeMillis() + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + zipName);

        // 处理重名文件：同名时追加序号
        Map<String, Integer> nameCount = new HashMap<>();

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (ProductMediaFile file : files) {
                String entryName = file.getOriginalName() != null ? file.getOriginalName() : file.getObjectName();
                // 去重：同名文件追加 (2), (3)...
                int count = nameCount.getOrDefault(entryName, 0) + 1;
                nameCount.put(entryName, count);
                if (count > 1) {
                    int dotIdx = entryName.lastIndexOf('.');
                    if (dotIdx > 0) {
                        entryName = entryName.substring(0, dotIdx) + " (" + count + ")" + entryName.substring(dotIdx);
                    } else {
                        entryName = entryName + " (" + count + ")";
                    }
                }

                try (InputStream is = minioService.getObject(bucket, file.getObjectName())) {
                    zos.putNextEntry(new ZipEntry(entryName));
                    is.transferTo(zos);
                    zos.closeEntry();
                } catch (Exception e) {
                    log.warn("批量下载跳过文件: id={}, name={}, error={}", file.getId(), entryName, e.getMessage());
                }
            }
            zos.flush();
        } catch (Exception e) {
            log.error("批量下载ZIP失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // ─── Helpers ───

    private Long getUserId(HttpServletRequest request) {
        Object uid = request.getAttribute("userId");
        return uid != null ? Long.parseLong(uid.toString()) : null;
    }

    /**
     * 前端 tag 参数兼容：main-image -> main_image
     */
    private String normalizeCategoryParam(String tag) {
        if (tag == null) return "main_image";
        return tag.contains("-") ? tag.replace("-", "_") : tag;
    }
}
