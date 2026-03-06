package com.logistics.track17.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品模板实体类
 */
@Data
@TableName(value = "product_template", autoResultMap = true)
public class ProductTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 模板名称 (可编辑)
     */
    private String templateName;

    /**
     * 模板版本
     */
    private String templateVersion;

    /**
     * 店铺标识符 (可编辑)
     */
    private String storeIdentifier;

    /**
     * 产品的 product.xxx.json 文件内容
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object productJsonContent;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
