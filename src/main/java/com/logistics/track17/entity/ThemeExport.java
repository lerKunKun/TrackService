package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 主题导出记录实体
 */
@Data
public class ThemeExport {

    private Long id;

    /** 关联的迁移ID */
    private Long migrationId;

    /** 导出的ZIP存储路径 */
    private String exportZipPath;

    /** 文件名 */
    private String fileName;

    /** 文件大小(bytes) */
    private Long fileSize;

    /** 导出用户 */
    private String exportedBy;

    /** 导出时间 */
    private LocalDateTime exportedAt;

    /** 下载次数 */
    private Integer downloadedCount;

    /** 最后下载时间 */
    private LocalDateTime lastDownloadedAt;
}
