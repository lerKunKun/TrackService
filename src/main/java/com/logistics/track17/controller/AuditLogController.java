package com.logistics.track17.controller;

import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.dto.PageResult;
import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.AuditLogEntity;
import com.logistics.track17.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/audit-logs")
@RequireAuth(admin = true)
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public Result<PageResult<AuditLogEntity>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        var list = auditLogService.getAuditLogs(page, pageSize, userId, operation, module, result, startTime, endTime);
        var total = auditLogService.countAuditLogs(userId, operation, module, result, startTime, endTime);
        return Result.success(PageResult.of(list, total, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<AuditLogEntity> detail(@PathVariable Long id) {
        AuditLogEntity auditLog = auditLogService.getById(id);
        if (auditLog == null) {
            return Result.error(404, "日志不存在");
        }
        return Result.success(auditLog);
    }
}
