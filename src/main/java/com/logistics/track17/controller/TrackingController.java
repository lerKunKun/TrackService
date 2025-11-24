package com.logistics.track17.controller;

import com.logistics.track17.dto.*;
import com.logistics.track17.service.TrackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运单控制器
 */
@Slf4j
@RestController
@RequestMapping("/tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    /**
     * 手动添加运单
     */
    @PostMapping
    public Result<TrackingResponse> create(@Validated @RequestBody TrackingRequest request) {
        TrackingResponse response = trackingService.create(request);
        return Result.success("运单添加成功", response);
    }

    /**
     * 获取运单列表
     */
    @GetMapping
    public Result<PageResult<TrackingResponse>> getList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String carrierCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {

        PageResult<TrackingResponse> result = trackingService.getList(
                keyword, shopId, status, carrierCode, startDate, endDate, page, pageSize
        );
        return Result.success(result);
    }

    /**
     * 获取已有的承运商列表（用于筛选）
     */
    @GetMapping("/carriers")
    public Result<List<String>> getUsedCarriers() {
        List<String> carriers = trackingService.getUsedCarriers();
        return Result.success(carriers);
    }

    /**
     * 获取运单详情
     */
    @GetMapping("/{id}")
    public Result<TrackingResponse> getById(@PathVariable Long id) {
        TrackingResponse response = trackingService.getById(id);
        return Result.success(response);
    }

    /**
     * 手动同步运单状态
     */
    @PostMapping("/{id}/sync")
    public Result<TrackingResponse> sync(@PathVariable Long id) {
        TrackingResponse response = trackingService.sync(id);
        return Result.success("同步成功", response);
    }

    /**
     * 更新备注
     */
    @PutMapping("/{id}/remarks")
    public Result<TrackingResponse> updateRemarks(
            @PathVariable Long id,
            @RequestBody UpdateRemarksRequest request) {
        TrackingResponse response = trackingService.updateRemarks(id, request.getRemarks());
        return Result.success("备注更新成功", response);
    }

    /**
     * 删除运单
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        trackingService.delete(id);
        return Result.success("运单删除成功", null);
    }

    /**
     * 批量删除运单
     */
    @PostMapping("/batch-delete")
    public Result<Void> deleteBatch(@RequestBody BatchDeleteRequest request) {
        trackingService.deleteBatch(request.getIds());
        return Result.success("批量删除成功", null);
    }

    /**
     * 批量导入运单
     */
    @PostMapping("/batch-import")
    public Result<BatchImportResult> batchImport(@Validated @RequestBody BatchImportRequest request) {
        BatchImportResult result = trackingService.batchImport(request);
        return Result.success(result.getMessage(), result);
    }
}
