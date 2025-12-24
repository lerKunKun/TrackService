package com.logistics.track17.dto;

import lombok.Data;

/**
 * 产品搜索请求DTO
 */
@Data
public class ProductSearchRequest {
    /**
     * 产品标题 (模糊查询)
     */
    private String title;

    /**
     * 标签 (模糊查询)
     */
    private String tags;

    /**
     * 上架状态: 0-草稿, 1-已上架
     */
    private Integer published;

    /**
     * 所属商店ID
     */
    private Long shopId;

    /**
     * 当前页码 (从1开始)
     */
    private Integer page = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 20;

    /**
     * 计算偏移量
     */
    public Integer getOffset() {
        return (page - 1) * pageSize;
    }
}
