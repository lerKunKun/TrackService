package com.logistics.track17.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.logistics.track17.dto.ProductTemplateDTO;
import com.logistics.track17.entity.Product;
import com.logistics.track17.entity.ProductMediaFile;
import com.logistics.track17.entity.ProductTemplate;
import com.logistics.track17.entity.Shop;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.mapper.ProductMapper;
import com.logistics.track17.mapper.ProductTemplateMapper;
import com.logistics.track17.mapper.ShopMapper;
import com.logistics.track17.service.ProductMediaFileService;
import com.logistics.track17.service.ProductTemplateService;
import com.logistics.track17.service.ShopifyProductPushService;
import com.logistics.track17.service.ShopifyThemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTemplateServiceImpl extends ServiceImpl<ProductTemplateMapper, ProductTemplate>
        implements ProductTemplateService {

    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;
    private final ShopifyThemeService shopifyThemeService;
    private final ShopifyProductPushService shopifyProductPushService;
    private final ProductMediaFileService productMediaFileService;

    @Override
    public Page<ProductTemplateDTO> getProductTemplatePage(Page<ProductTemplate> page) {
        Page<ProductTemplateDTO> resultPage = new Page<>(page.getCurrent(), page.getSize());

        Page<Product> productPage = new Page<>(page.getCurrent(), page.getSize());
        productMapper.selectPage(productPage, new QueryWrapper<Product>().orderByDesc("id"));

        resultPage.setTotal(productPage.getTotal());

        if (productPage.getRecords().isEmpty()) {
            return resultPage;
        }

        List<Long> productIds = productPage.getRecords().stream()
                .map(Product::getId).collect(Collectors.toList());

        List<ProductTemplate> templateList = this
                .list(new QueryWrapper<ProductTemplate>().in("product_id", productIds));
        Map<Long, ProductTemplate> templateMap = templateList.stream()
                .collect(Collectors.toMap(ProductTemplate::getProductId, t -> t));

        // 收集所有 sourceShopId 用于批量查询店铺名称
        Set<Long> shopIds = templateList.stream()
                .map(ProductTemplate::getSourceShopId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> shopNameMap = new HashMap<>();
        if (!shopIds.isEmpty()) {
            for (Long shopId : shopIds) {
                Shop shop = shopMapper.selectById(shopId);
                if (shop != null) {
                    shopNameMap.put(shopId, shop.getShopName());
                }
            }
        }

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
                dto.setSourceShopId(template.getSourceShopId());
                dto.setSourceShopName(shopNameMap.get(template.getSourceShopId()));
                dto.setLastPullTime(template.getLastPullTime());
                dto.setLastPushTime(template.getLastPushTime());
            }
            return dto;
        }).collect(Collectors.toList());

        resultPage.setRecords(dtoList);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplateInfo(Long productId, String templateName, Long sourceShopId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw BusinessException.of("产品不存在: " + productId);
        }

        ProductTemplate template = this.getOne(
                new QueryWrapper<ProductTemplate>().eq("product_id", productId));
        if (template != null) {
            if (templateName != null) {
                template.setTemplateName(templateName.trim());
            }
            if (sourceShopId != null) {
                template.setSourceShopId(sourceShopId);
            }
            return this.updateById(template);
        } else {
            template = new ProductTemplate();
            template.setProductId(productId);
            template.setTemplateName(templateName != null ? templateName.trim() : null);
            template.setSourceShopId(sourceShopId);
            return this.save(template);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pullThemeFiles(Long productId) {
        ProductTemplate template = getOrCreateTemplate(productId);

        if (template.getSourceShopId() == null) {
            throw BusinessException.of("请先为该产品设置源店铺");
        }

        Shop sourceShop = shopMapper.selectDetailById(template.getSourceShopId());
        if (sourceShop == null) {
            throw BusinessException.of("源店铺不存在 (id=" + template.getSourceShopId() + ")");
        }
        if (sourceShop.getAccessToken() == null) {
            throw BusinessException.of("源店铺未授权，请先完成 OAuth 授权");
        }

        Long themeId = shopifyThemeService.getPublishedThemeId(sourceShop);
        String templateName = template.getTemplateName();
        if (templateName == null || templateName.isBlank()) {
            throw BusinessException.of("请先设置模板名称（用于定位 product.xxx.json）");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> pulled = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // 1. settings_data.json
        try {
            String settings = shopifyThemeService.getAsset(sourceShop, themeId, "config/settings_data.json");
            template.setThemeSettings(settings);
            pulled.add("config/settings_data.json");
        } catch (Exception e) {
            errors.add("config/settings_data.json: " + e.getMessage());
        }

        // 2. footer-group.json
        try {
            String footer = shopifyThemeService.getAsset(sourceShop, themeId, "sections/footer-group.json");
            template.setFooterGroup(footer);
            pulled.add("sections/footer-group.json");
        } catch (Exception e) {
            errors.add("sections/footer-group.json: " + e.getMessage());
        }

        // 3. product.{templateName}.json
        String productAssetKey = "templates/product." + templateName + ".json";
        try {
            String productJson = shopifyThemeService.getAsset(sourceShop, themeId, productAssetKey);
            template.setProductJsonContent(productJson);
            pulled.add(productAssetKey);
        } catch (Exception e) {
            errors.add(productAssetKey + ": " + e.getMessage());
        }

        template.setLastPullTime(LocalDateTime.now());
        template.setTemplateVersion(String.valueOf(themeId));
        this.updateById(template);

        result.put("pulled", pulled);
        result.put("errors", errors);
        result.put("themeId", themeId);
        result.put("lastPullTime", template.getLastPullTime());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generatePreviewUrl(Long productId) {
        ProductTemplate template = getOrCreateTemplate(productId);

        // 1. 找到开发者店铺
        Shop devShop = shopMapper.selectDevStore();
        if (devShop == null) {
            throw BusinessException.of("未配置开发者店铺，请在店铺管理中标记一个开发者店铺");
        }
        if (devShop.getAccessToken() == null) {
            throw BusinessException.of("开发者店铺未授权，请先完成 OAuth 授权");
        }

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw BusinessException.of("产品不存在: " + productId);
        }

        // 2. 获取产品媒体文件
        List<ProductMediaFile> mediaFiles = productMediaFileService.listByProduct(productId);

        // 3. 推送产品到开发者店铺
        Long devProductId = shopifyProductPushService.pushProduct(
                devShop, product, mediaFiles, template.getDevProductId());
        template.setDevProductId(devProductId);

        // 4. 推送主题文件
        Long themeId = shopifyThemeService.getPublishedThemeId(devShop);

        if (template.getThemeSettings() != null) {
            shopifyThemeService.putAsset(devShop, themeId,
                    "config/settings_data.json", template.getThemeSettings().toString());
        }
        if (template.getFooterGroup() != null) {
            shopifyThemeService.putAsset(devShop, themeId,
                    "sections/footer-group.json", template.getFooterGroup().toString());
        }
        if (template.getProductJsonContent() != null && template.getTemplateName() != null) {
            shopifyThemeService.putAsset(devShop, themeId,
                    "templates/product." + template.getTemplateName() + ".json",
                    template.getProductJsonContent().toString());
        }

        template.setLastPushTime(LocalDateTime.now());
        this.updateById(template);

        // 5. 构建预览 URL
        String handle = product.getHandle() != null ? product.getHandle() : "product-" + productId;
        return String.format("https://%s/products/%s?preview_theme_id=%d",
                devShop.getShopDomain(), handle, themeId);
    }

    @Override
    public Map<String, Object> getThemeFilesInfo(Long productId) {
        ProductTemplate template = this.getOne(
                new QueryWrapper<ProductTemplate>().eq("product_id", productId));
        if (template == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("hasSettings", template.getThemeSettings() != null);
        info.put("hasFooterGroup", template.getFooterGroup() != null);
        info.put("hasProductJson", template.getProductJsonContent() != null);
        info.put("templateName", template.getTemplateName());
        info.put("lastPullTime", template.getLastPullTime());
        info.put("lastPushTime", template.getLastPushTime());
        info.put("devProductId", template.getDevProductId());

        if (template.getThemeSettings() != null) {
            String s = template.getThemeSettings().toString();
            info.put("settingsPreview", s.length() > 200 ? s.substring(0, 200) + "..." : s);
        }
        if (template.getFooterGroup() != null) {
            String s = template.getFooterGroup().toString();
            info.put("footerGroupPreview", s.length() > 200 ? s.substring(0, 200) + "..." : s);
        }
        if (template.getProductJsonContent() != null) {
            String s = template.getProductJsonContent().toString();
            info.put("productJsonPreview", s.length() > 200 ? s.substring(0, 200) + "..." : s);
        }

        return info;
    }

    private ProductTemplate getOrCreateTemplate(Long productId) {
        ProductTemplate template = this.getOne(
                new QueryWrapper<ProductTemplate>().eq("product_id", productId));
        if (template == null) {
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw BusinessException.of("产品不存在: " + productId);
            }
            template = new ProductTemplate();
            template.setProductId(productId);
            this.save(template);
        }
        return template;
    }
}
