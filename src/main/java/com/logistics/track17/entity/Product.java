package com.logistics.track17.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("products")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String handle;

    private String title;

    private String bodyHtml;

    private String vendor;

    private String tags;

    /** 上架状态: 0-草稿, 1-已上架 */
    private Integer published;

    private String productUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Integer publishStatus;

    @TableField(exist = false)
    private LocalDateTime lastExportTime;

    @TableField(exist = false)
    private String imageUrl;
}
