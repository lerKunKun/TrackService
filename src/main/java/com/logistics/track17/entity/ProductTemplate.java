package com.logistics.track17.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "product_template", autoResultMap = true)
public class ProductTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String templateName;

    private String templateVersion;

    private String storeIdentifier;

    /** 源店铺 ID（拉取主题文件的来源） */
    private Long sourceShopId;

    /** product.xxx.json 文件内容 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object productJsonContent;

    /** settings_data.json 缓存 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object themeSettings;

    /** footer-group.json 缓存 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object footerGroup;

    private LocalDateTime lastPullTime;

    private LocalDateTime lastPushTime;

    /** 推送到开发者店铺后创建的产品 ID */
    private Long devProductId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
