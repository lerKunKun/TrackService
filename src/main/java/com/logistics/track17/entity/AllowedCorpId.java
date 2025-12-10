package com.logistics.track17.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 允许登录的企业CorpId实体
 */
@Data
public class AllowedCorpId {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 企业CorpId
     */
    private String corpId;

    /**
     * 企业名称
     */
    private String corpName;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间戳
     */
    private LocalDateTime deletedAt;
}
