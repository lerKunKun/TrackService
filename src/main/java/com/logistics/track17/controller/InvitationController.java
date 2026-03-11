package com.logistics.track17.controller;

import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.Invitation;
import com.logistics.track17.service.InvitationService;
import com.logistics.track17.util.UserContextHolder;
import com.logistics.track17.util.RequestContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 邀请 Controller
 * 管理邮箱邀请相关 API
 */
@RestController
@RequestMapping("/invitations")
@RequireAuth
@Slf4j
public class InvitationController {

    @Autowired
    private InvitationService invitationService;

    /**
     * 创建邀请
     */
    @PostMapping
    @RequireAuth(admin = true)
    public Result<Invitation> createInvitation(@RequestBody CreateInvitationRequest req) {
        Long invitedBy = UserContextHolder.getCurrentUserId();
        if (invitedBy == null) {
            return Result.error(401, "未登录");
        }

        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            return Result.error(400, "邮箱不能为空");
        }

        // 使用 RequestContext 中的 companyId，直接从当前公司上下文获取
        Long companyId = RequestContext.getCurrentCompanyId();

        // 如果请求中指定了 companyId 则优先使用
        if (req.getCompanyId() != null) {
            companyId = req.getCompanyId();
        }

        // 如果仍然没有 companyId，使用默认公司 ID = 1
        if (companyId == null) {
            companyId = 1L;
        }

        Invitation invitation = invitationService.createInvitation(
                companyId,
                req.getShopId(),
                req.getEmail().trim(),
                req.getRoleId(),
                invitedBy,
                req.getExpiresInDays() != null ? req.getExpiresInDays() : 7);

        return Result.success(invitation);
    }

    /**
     * 查询公司的邀请列表
     */
    @GetMapping
    @RequireAuth(admin = true)
    public Result<List<Invitation>> getInvitations(@RequestParam(required = false) Long companyId) {
        if (companyId == null) {
            companyId = RequestContext.getCurrentCompanyId();
        }
        if (companyId == null) {
            companyId = 1L;
        }

        List<Invitation> list = invitationService.getInvitationsByCompany(companyId);
        return Result.success(list);
    }

    /**
     * 通过 Token 查看邀请详情（公开接口，被邀请人使用）
     */
    @GetMapping("/token/{token}")
    public Result<Invitation> getByToken(@PathVariable String token) {
        Invitation invitation = invitationService.getByToken(token);
        if (invitation == null) {
            return Result.error(404, "邀请不存在");
        }
        return Result.success(invitation);
    }

    /**
     * 接受邀请
     */
    @PostMapping("/accept/{token}")
    public Result<String> acceptInvitation(@PathVariable String token) {
        Long userId = UserContextHolder.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录后再接受邀请");
        }

        invitationService.acceptInvitation(token, userId);
        return Result.success("邀请接受成功");
    }

    /**
     * 撤销邀请
     */
    @PostMapping("/{id}/revoke")
    @RequireAuth(admin = true)
    public Result<String> revokeInvitation(@PathVariable Long id) {
        invitationService.revokeInvitation(id);
        return Result.success("邀请已撤销");
    }

    @Data
    static class CreateInvitationRequest {
        private Long companyId;
        private Long shopId;
        private String email;
        private Long roleId;
        private Integer expiresInDays;
    }
}
