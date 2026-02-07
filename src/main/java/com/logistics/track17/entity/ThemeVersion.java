package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 主题版本存档实体
 */
@Data
public class ThemeVersion {

    private Long id;

    /** 主题名称，如 Shrine Pro */
    private String themeName;

    /** 版本号，如 2.9.5 */
    private String version;

    /** ZIP文件存储路径（MinIO或本地） */
    private String zipFilePath;

    /** ZIP文件大小(bytes) */
    private Long zipFileSize;

    /** sections文件数量 */
    private Integer sectionsCount;

    /** 是否当前使用版本 */
    private Boolean isCurrent;

    /** 上传用户 */
    private String uploadedBy;

    /** 上传时间 */
    private LocalDateTime uploadedAt;

    /** 版本说明 */
    private String notes;
}
