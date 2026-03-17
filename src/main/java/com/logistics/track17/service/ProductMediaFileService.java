package com.logistics.track17.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.logistics.track17.entity.ProductMediaFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ProductMediaFileService extends IService<ProductMediaFile> {

    /**
     * 按产品+分类查询文件列表（按 sort_order 排序）
     */
    List<ProductMediaFile> listByProductAndCategory(Long productId, String category);

    /**
     * 按产品查询所有文件
     */
    List<ProductMediaFile> listByProduct(Long productId);

    /**
     * 批量统计各产品各分类的文件数量
     * 返回 Map<productId, Map<category, count>>
     */
    Map<Long, Map<String, Long>> countByProductIds(List<Long> productIds);

    /**
     * 上传文件：MinIO 存储 + DB 写入记录
     */
    ProductMediaFile uploadFile(Long productId, String category, String tags, MultipartFile file, Long uploaderId);

    /**
     * 从 URL 下载并保存：MinIO 存储 + DB 写入记录
     */
    ProductMediaFile downloadFromUrl(Long productId, String category, String tags, String url, Long uploaderId);

    /**
     * 删除文件：MinIO 删除 + DB 删除记录
     */
    void deleteFile(Long fileId);

    /**
     * 批量删除文件
     */
    void batchDeleteFiles(Long productId, List<Long> fileIds);

    /**
     * 更新排序（接收有序 ID 列表）
     */
    void updateSort(Long productId, String category, List<Long> sortedIds);

    /**
     * 移动文件到其他分类
     */
    void moveCategory(Long fileId, String newCategory);

    /**
     * 同步 Shopify 产品主图到 MinIO+DB
     */
    Map<String, Object> syncProductImages(Long productId, Long uploaderId);

    /**
     * 迁移 MinIO 存量数据到 DB（一次性）
     */
    Map<String, Object> migrateFromMinio();

    /**
     * 修复已有记录的 URL（将 R2 API endpoint URL 替换为 CDN URL）
     */
    Map<String, Object> fixUrls();
}
