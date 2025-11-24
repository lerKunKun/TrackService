package com.logistics.track17.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量导入运单请求
 */
@Data
public class BatchImportRequest {

    @NotEmpty(message = "导入列表不能为空")
    @Valid
    private List<BatchImportItem> items;
}
