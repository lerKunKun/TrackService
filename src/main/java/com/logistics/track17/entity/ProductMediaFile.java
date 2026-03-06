package com.logistics.track17.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_media_files")
public class ProductMediaFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** main_image | detail_media | ad_media | document */
    private String category;

    private String originalName;

    /** MinIO object path: {productId}/{category}/{uuid}.ext */
    private String objectName;

    private String contentType;

    private Long fileSize;

    /** image | video | document */
    private String mediaType;

    private String url;

    /** UPLOAD | URL_DOWNLOAD | SHOPIFY_SYNC | SUPPLIER */
    private String source;

    private String sourceUrl;

    private Integer sortOrder;

    private Long uploaderId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
