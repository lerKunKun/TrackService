package com.logistics.track17.controller;

import com.logistics.track17.dto.PageResult;
import com.logistics.track17.dto.Result;
import com.logistics.track17.dto.ShopRequest;
import com.logistics.track17.dto.ShopResponse;
import com.logistics.track17.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 店铺控制器
 */
@Slf4j
@RestController
@RequestMapping("/shops")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    /**
     * 创建店铺
     */
    @PostMapping
    public Result<ShopResponse> create(@Validated @RequestBody ShopRequest request) {
        ShopResponse response = shopService.create(request);
        return Result.success("店铺创建成功", response);
    }

    /**
     * 获取店铺列表
     */
    @GetMapping
    public Result<PageResult<ShopResponse>> getList(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        PageResult<ShopResponse> result = shopService.getList(platform, page, pageSize);
        return Result.success(result);
    }

    /**
     * 获取店铺详情
     */
    @GetMapping("/{id}")
    public Result<ShopResponse> getById(@PathVariable Long id) {
        ShopResponse response = shopService.getById(id);
        return Result.success(response);
    }

    /**
     * 更新店铺
     */
    @PutMapping("/{id}")
    public Result<ShopResponse> update(@PathVariable Long id,
                                       @Validated @RequestBody ShopRequest request) {
        ShopResponse response = shopService.update(id, request);
        return Result.success("店铺更新成功", response);
    }

    /**
     * 删除店铺
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shopService.delete(id);
        return Result.success("店铺删除成功", null);
    }
}
