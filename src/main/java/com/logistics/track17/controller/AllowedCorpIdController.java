package com.logistics.track17.controller;

import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.dto.AllowedCorpIdRequest;
import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.AllowedCorpId;
import com.logistics.track17.service.AllowedCorpIdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 允许登录的企业CorpId管理控制器（仅管理员可访问）
 */
@Slf4j
@RestController
@RequestMapping("/allowed-corp-ids")
public class AllowedCorpIdController {

    private final AllowedCorpIdService allowedCorpIdService;

    public AllowedCorpIdController(AllowedCorpIdService allowedCorpIdService) {
        this.allowedCorpIdService = allowedCorpIdService;
    }

    /**
     * 获取所有允许的CorpId列表
     */
    @GetMapping
    @RequireAuth(admin = true)
    public Result<List<AllowedCorpId>> getAllAllowedCorpIds() {
        log.info("Admin requesting all allowed corp IDs");
        List<AllowedCorpId> corpIds = allowedCorpIdService.getAllAllowedCorpIds();
        return Result.success(corpIds);
    }

    /**
     * 添加允许的CorpId
     */
    @PostMapping
    @RequireAuth(admin = true)
    public Result<Void> addAllowedCorpId(@Validated @RequestBody AllowedCorpIdRequest request,
            @RequestAttribute("username") String username) {
        log.info("Admin {} adding allowed corpId: {}", username, request.getCorpId());

        AllowedCorpId allowedCorpId = new AllowedCorpId();
        allowedCorpId.setCorpId(request.getCorpId());
        allowedCorpId.setCorpName(request.getCorpName());
        allowedCorpId.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        allowedCorpId.setCreatedBy(username);

        allowedCorpIdService.addAllowedCorpId(allowedCorpId);
        return Result.success();
    }

    /**
     * 删除允许的CorpId
     */
    @DeleteMapping("/{corpId}")
    @RequireAuth(admin = true)
    public Result<Void> removeAllowedCorpId(@PathVariable String corpId,
            @RequestAttribute("username") String username) {
        log.info("Admin {} removing allowed corpId: {}", username, corpId);
        allowedCorpIdService.removeAllowedCorpId(corpId);
        return Result.success();
    }

    /**
     * 更新CorpId状态（启用/禁用）
     */
    @PutMapping("/{corpId}/status")
    @RequireAuth(admin = true)
    public Result<Void> updateCorpIdStatus(@PathVariable String corpId,
            @RequestParam Integer status,
            @RequestAttribute("username") String username) {
        log.info("Admin {} updating corpId {} status to: {}", username, corpId, status);
        allowedCorpIdService.updateCorpIdStatus(corpId, status);
        return Result.success();
    }
}
