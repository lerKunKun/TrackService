package com.logistics.track17.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.logistics.track17.dto.ProductTemplateDTO;
import com.logistics.track17.entity.Product;
import com.logistics.track17.entity.ProductTemplate;
import com.logistics.track17.mapper.ProductMapper;
import com.logistics.track17.mapper.ProductTemplateMapper;
import com.logistics.track17.service.ProductTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTemplateServiceImpl extends ServiceImpl<ProductTemplateMapper, ProductTemplate>
        implements ProductTemplateService {

    private final ProductMapper productMapper;

    @Override
    public Page<ProductTemplateDTO> getProductTemplatePage(Page<ProductTemplate> page) {
        Page<ProductTemplateDTO> resultPage = new Page<>(page.getCurrent(), page.getSize());

        Page<Product> productPage = new Page<>(page.getCurrent(), page.getSize());
        productMapper.selectPage(productPage, new QueryWrapper<Product>().orderByDesc("id"));

        resultPage.setTotal(productPage.getTotal());

        if (productPage.getRecords().isEmpty()) {
            return resultPage;
        }

        List<Long> productIds = productPage.getRecords().stream().map(Product::getId).collect(Collectors.toList());

        List<ProductTemplate> templateList = this
                .list(new QueryWrapper<ProductTemplate>().in("product_id", productIds));
        Map<Long, ProductTemplate> templateMap = templateList.stream()
                .collect(Collectors.toMap(ProductTemplate::getProductId, t -> t));

        List<ProductTemplateDTO> dtoList = productPage.getRecords().stream().map(product -> {
            ProductTemplateDTO dto = new ProductTemplateDTO();
            dto.setProductId(product.getId());
            dto.setProductName(product.getTitle());

            ProductTemplate template = templateMap.get(product.getId());
            if (template != null) {
                dto.setId(template.getId());
                dto.setTemplateName(template.getTemplateName());
                dto.setTemplateVersion(template.getTemplateVersion());
                dto.setStoreIdentifier(template.getStoreIdentifier());
            }
            return dto;
        }).collect(Collectors.toList());

        resultPage.setRecords(dtoList);
        return resultPage;
    }

    @Override
    public boolean updateTemplateInfo(Long productId, String templateName, String storeIdentifier) {
        ProductTemplate template = this.getOne(new QueryWrapper<ProductTemplate>().eq("product_id", productId));
        if (template != null) {
            template.setTemplateName(templateName);
            template.setStoreIdentifier(storeIdentifier);
            return this.updateById(template);
        } else {
            template = new ProductTemplate();
            template.setProductId(productId);
            template.setTemplateName(templateName);
            template.setStoreIdentifier(storeIdentifier);
            return this.save(template);
        }
    }

    @Override
    public String generatePreviewUrl(Long productId) {
        // TBD: Actual logic to push media, CSV, and theme JSON files.
        // Step 1: Push media and CSV
        log.info("Pushing media/CSV for product {}", productId);
        // Step 2: Push settings_data.json, footer-group.json, product.xxx.json to
        // target theme
        log.info("Pushing theme json for product {}", productId);

        // Mocking the generated URL for now
        ProductTemplate template = this.getOne(new QueryWrapper<ProductTemplate>().eq("product_id", productId));
        String store = template != null && template.getStoreIdentifier() != null ? template.getStoreIdentifier()
                : "demo-store";

        Product product = productMapper.selectById(productId);
        String handle = product != null ? product.getHandle() : "demo-product";

        return "https://" + store + ".myshopify.com/products/" + handle + "?preview_theme_id=123456789";
    }
}
