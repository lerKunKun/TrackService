package com.logistics.track17.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "product_media", autoResultMap = true)
public class ProductMedia {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> referenceLink;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> mainImages;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> detailMedia;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> adMedia;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
