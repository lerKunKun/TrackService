package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 产品导入记录实体类
 * 记录每次CSV导入操作
 */
@Data
public class ProductImport {
    /**
     * 导入记录ID
     */
    private Long id;

    /**
     * 导入文件名
     */
    private String fileName;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 导入状态: 0-进行中, 1-成功, 2-失败
     */
    private Integer status;

    /**
     * 总记录数
     */
    private Integer totalRecords;

    /**
     * 成功导入记录数
     */
    private Integer successRecords;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
}
