package com.logistics.track17.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量删除请求
 */
@Data
public class BatchDeleteRequest {

    @NotEmpty(message = "ID列表不能为空")
    private List<Long> ids;
}
