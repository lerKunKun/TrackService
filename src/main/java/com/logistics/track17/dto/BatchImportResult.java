package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量导入结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportResult {

    private int total;           // 总数
    private int success;         // 成功数
    private int failed;          // 失败数
    private String message;      // 消息
}
