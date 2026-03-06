package com.logistics.track17.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.logistics.track17.dto.ProductTemplateDTO;
import com.logistics.track17.entity.ProductTemplate;

public interface ProductTemplateService extends IService<ProductTemplate> {

    /**
     * 分页查询产品模板列表
     */
    Page<ProductTemplateDTO> getProductTemplatePage(Page<ProductTemplate> page);

    /**
     * 更新模板名称和店铺标识符
     */
    boolean updateTemplateInfo(Long productId, String templateName, String storeIdentifier);

    /**
     * 预览: 推送媒体/CSV/JSON, 并返回预览链接
     */
    String generatePreviewUrl(Long productId);
}
