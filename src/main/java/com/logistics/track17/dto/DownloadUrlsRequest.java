package com.logistics.track17.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class DownloadUrlsRequest {

    @NotBlank(message = "tag 不能为空")
    private String tag;

    @NotEmpty(message = "urls 不能为空")
    private List<String> urls;

    /** 自定义标签（逗号分隔），可选 */
    private String tags;
}
