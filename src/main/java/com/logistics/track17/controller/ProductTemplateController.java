package com.logistics.track17.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.dto.ProductTemplateDTO;
import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.ProductTemplate;
import com.logistics.track17.entity.Shop;
import com.logistics.track17.mapper.ShopMapper;
import com.logistics.track17.service.ProductTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/product-templates")
@RequiredArgsConstructor
@RequireAuth
@Api(tags = "产品模板管理")
public class ProductTemplateController {

    private final ProductTemplateService productTemplateService;
    private final ShopMapper shopMapper;

    @GetMapping("/list")
    @ApiOperation("分页获取产品模板列表")
    public Result<Page<ProductTemplateDTO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<ProductTemplate> page = new Page<>(current, size);
        return Result.success(productTemplateService.getProductTemplatePage(page));
    }

    @PutMapping("/{productId}")
    @ApiOperation("更新产品模板信息（模板名 / 源店铺）")
    public Result<Boolean> updateTemplateInfo(
            @PathVariable Long productId,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) Long sourceShopId) {
        return Result.success(productTemplateService.updateTemplateInfo(productId, templateName, sourceShopId));
    }

    @PostMapping("/{productId}/pull-theme")
    @ApiOperation("从源店铺拉取主题文件")
    public Result<Map<String, Object>> pullThemeFiles(@PathVariable Long productId) {
        return Result.success(productTemplateService.pullThemeFiles(productId));
    }

    @PostMapping("/{productId}/preview")
    @ApiOperation("推送产品+主题到开发者店铺并返回预览链接")
    public Result<String> previewTemplate(@PathVariable Long productId) {
        return Result.success(productTemplateService.generatePreviewUrl(productId));
    }

    @GetMapping("/{productId}/theme-files")
    @ApiOperation("查看已缓存的主题文件信息")
    public Result<Map<String, Object>> getThemeFiles(@PathVariable Long productId) {
        return Result.success(productTemplateService.getThemeFilesInfo(productId));
    }

    // ========== 开发者店铺管理 ==========

    @GetMapping("/dev-store")
    @ApiOperation("获取当前开发者店铺")
    public Result<Shop> getDevStore() {
        Shop devStore = shopMapper.selectDevStore();
        return Result.success(devStore);
    }

    @PutMapping("/dev-store/{shopId}")
    @ApiOperation("设置开发者店铺（同一时间仅一个）")
    public Result<Boolean> setDevStore(@PathVariable Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            return Result.error(404, "店铺不存在");
        }
        shopMapper.clearDevStoreFlag();
        shopMapper.setDevStoreFlag(shopId);
        return Result.success(true);
    }
}
