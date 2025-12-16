package com.logistics.track17.controller;

import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.DingtalkSyncLog;
import com.logistics.track17.mapper.DingtalkSyncLogMapper;
import com.logistics.track17.service.DingtalkSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钉钉同步控制器
 * 提供手动触发同步和查看同步日志的接口
 */
@RestController
@RequestMapping("/dingtalk/sync")
@Slf4j
public class DingtalkSyncController {

    @Autowired
    private DingtalkSyncService dingtalkSyncService;

    @Autowired
    private DingtalkSyncLogMapper dingtalkSyncLogMapper;

    /**
     * 手动触发全量同步
     * POST /api/v1/dingtalk/sync/full
     */
    @PostMapping("/full")
    public ResponseEntity<Result<DingtalkSyncLog>> triggerFullSync() {
        log.info("收到全量同步请求");
        try {
            DingtalkSyncLog result = dingtalkSyncService.fullSync();
            return ResponseEntity.ok(Result.success("同步完成", result));
        } catch (Exception e) {
            log.error("全量同步失败", e);
            return ResponseEntity.ok(Result.error(500, "同步失败: " + e.getMessage()));
        }
    }

    /**
     * 仅同步部门
     * POST /api/v1/dingtalk/sync/departments
     */
    @PostMapping("/departments")
    public ResponseEntity<Result<Integer>> syncDepartments() {
        log.info("收到部门同步请求");
        try {
            int count = dingtalkSyncService.syncDepartments();
            return ResponseEntity.ok(Result.success("部门同步完成，共处理 " + count + " 个部门", count));
        } catch (Exception e) {
            log.error("部门同步失败", e);
            return ResponseEntity.ok(Result.error(500, "部门同步失败: " + e.getMessage()));
        }
    }

    /**
     * 仅同步用户
     * POST /api/v1/dingtalk/sync/users
     */
    @PostMapping("/users")
    public ResponseEntity<Result<Integer>> syncUsers() {
        log.info("收到用户同步请求");
        try {
            int count = dingtalkSyncService.syncUsers();
            return ResponseEntity.ok(Result.success("用户同步完成，共处理 " + count + " 个用户", count));
        } catch (Exception e) {
            log.error("用户同步失败", e);
            return ResponseEntity.ok(Result.error(500, "用户同步失败: " + e.getMessage()));
        }
    }

    /**
     * 应用角色映射规则
     * POST /api/v1/dingtalk/sync/roles
     */
    @PostMapping("/roles")
    public ResponseEntity<Result<Integer>> applyRoles() {
        log.info("收到角色映射请求");
        try {
            int count = dingtalkSyncService.applyRoleMappingRules();
            return ResponseEntity.ok(Result.success("角色映射完成，共处理 " + count + " 个用户", count));
        } catch (Exception e) {
            log.error("角色映射失败", e);
            return ResponseEntity.ok(Result.error(500, "角色映射失败: " + e.getMessage()));
        }
    }

    /**
     * 获取同步日志列表
     * GET /api/v1/dingtalk/sync/logs
     */
    @GetMapping("/logs")
    public ResponseEntity<Result<List<DingtalkSyncLog>>> getSyncLogs(
            @RequestParam(defaultValue = "50") Integer limit) {
        log.info("查询同步日志，limit: {}", limit);
        try {
            List<DingtalkSyncLog> logs = dingtalkSyncLogMapper.selectRecent(limit);
            return ResponseEntity.ok(Result.success(logs));
        } catch (Exception e) {
            log.error("查询同步日志失败", e);
            return ResponseEntity.ok(Result.error(500, "查询同步日志失败: " + e.getMessage()));
        }
    }

    /**
     * 获取同步日志详情
     * GET /api/v1/dingtalk/sync/logs/{id}
     */
    @GetMapping("/logs/{id}")
    public ResponseEntity<Result<DingtalkSyncLog>> getSyncLogDetail(@PathVariable Long id) {
        log.info("查询同步日志详情，id: {}", id);
        try {
            DingtalkSyncLog log = dingtalkSyncLogMapper.selectById(id);
            if (log == null) {
                return ResponseEntity.ok(Result.error(404, "同步日志不存在"));
            }
            return ResponseEntity.ok(Result.success(log));
        } catch (Exception e) {
            log.error("查询同步日志详情失败", e);
            return ResponseEntity.ok(Result.error(500, "查询同步日志详情失败: " + e.getMessage()));
        }
    }
}
