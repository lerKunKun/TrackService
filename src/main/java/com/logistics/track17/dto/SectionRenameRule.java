package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Section重命名规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionRenameRule {

    /**
     * 旧Section名称
     */
    private String oldName;

    /**
     * 新Section名称
     */
    private String newName;

    /**
     * 置信度
     */
    private String confidence;

    /**
     * 重命名原因
     */
    private String reason;

    /**
     * 是否由Git检测到（RENAME类型的DiffEntry）
     */
    private Boolean gitDetected;

    /**
     * 名称相似度（如果是基于相似度推断的）
     */
    private Double nameSimilarity;
}
