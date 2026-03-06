package com.logistics.track17.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.logistics.track17.dto.Result;
import com.logistics.track17.dto.ProductTemplateDTO;
import com.logistics.track17.entity.ProductTemplate;
import com.logistics.track17.service.ProductTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product-templates")
@RequiredArgsConstructor
@Api(tags = "产品模板管理")
public class ProductTemplateController {

    private final ProductTemplateService productTemplateService;

    @GetMapping("/list")
    @ApiOperation(value = "分页获取产品模板列表")
    public Result<Page<ProductTemplateDTO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<ProductTemplate> page = new Page<>(current, size);
        return Result.success(productTemplateService.getProductTemplatePage(page));
    }

    @PutMapping("/{productId}")
    @ApiOperation(value = "更新产品模板信息(模板名/店铺)")
    public Result<Boolean> updateTemplateInfo(
            @PathVariable Long productId,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String storeIdentifier) {
        return Result.success(productTemplateService.updateTemplateInfo(productId, templateName, storeIdentifier));
    }

    @PostMapping("/{productId}/preview")
    @ApiOperation(value = "生成商店模板预览链接并推送素材")
    public Result<String> previewTemplate(@PathVariable Long productId) {
        return Result.success(productTemplateService.generatePreviewUrl(productId));
    }
}
