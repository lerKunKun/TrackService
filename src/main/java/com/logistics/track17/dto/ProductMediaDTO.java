package com.logistics.track17.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 产品媒体显示DTO（基于 MinIO 存储）
 */
@Data
public class ProductMediaDTO {
    private Long id; // product_media ID
    private Long productId; // 产品ID
    private String productName; // 产品名称
    private String description; // 描述
    private String handle; // Shopify Handle
    private String imageUrl; // 产品首图
    private List<String> referenceLink; // 对标页链接列表（多个）
    private Map<String, Long> tagFileCounts; // 各 tag 下的文件数量统计
}
