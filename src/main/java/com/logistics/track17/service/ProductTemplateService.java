package com.logistics.track17.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.logistics.track17.dto.ProductTemplateDTO;
import com.logistics.track17.entity.ProductTemplate;

import java.util.Map;

public interface ProductTemplateService extends IService<ProductTemplate> {

    Page<ProductTemplateDTO> getProductTemplatePage(Page<ProductTemplate> page);

    boolean updateTemplateInfo(Long productId, String templateName, Long sourceShopId);

    /**
     * 从源店铺拉取主题文件（settings_data / footer-group / product.xxx.json）
     */
    Map<String, Object> pullThemeFiles(Long productId);

    /**
     * 推送产品+媒体+主题文件到开发者店铺，返回预览 URL
     */
    String generatePreviewUrl(Long productId);

    /**
     * 查看已缓存的主题文件概要
     */
    Map<String, Object> getThemeFilesInfo(Long productId);
}
