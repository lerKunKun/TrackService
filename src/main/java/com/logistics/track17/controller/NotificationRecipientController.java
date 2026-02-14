package com.logistics.track17.controller;

import com.logistics.track17.entity.NotificationRecipient;
import com.logistics.track17.mapper.NotificationRecipientMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知接收人管理 Controller
 * 前端页面管理通知接收人（支持多人）
 */
@Slf4j
@RestController
@RequestMapping("/alert-config/recipients")
public class NotificationRecipientController {

    private final NotificationRecipientMapper recipientMapper;
    private final com.logistics.track17.service.DingtalkNotificationService dingtalkNotificationService;

    public NotificationRecipientController(NotificationRecipientMapper recipientMapper,
            com.logistics.track17.service.DingtalkNotificationService dingtalkNotificationService) {
        this.recipientMapper = recipientMapper;
        this.dingtalkNotificationService = dingtalkNotificationService;
    }

    /**
     * 获取所有接收人
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        List<NotificationRecipient> recipients = recipientMapper.findAll();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", recipients);
        return ResponseEntity.ok(result);
    }

    /**
     * 添加接收人
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody NotificationRecipient recipient) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (recipient.getIsEnabled() == null) {
                recipient.setIsEnabled(true);
            }
            if (recipient.getAlertTypes() == null || recipient.getAlertTypes().isEmpty()) {
                recipient.setAlertTypes("ALL");
            }
            recipientMapper.insert(recipient);
            result.put("success", true);
            result.put("data", recipient);
            log.info("添加通知接收人: {} ({})", recipient.getName(), recipient.getDingtalkUserid());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("添加通知接收人失败", e);
            result.put("success", false);
            result.put("message", "添加失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 更新接收人
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
            @RequestBody NotificationRecipient recipient) {
        Map<String, Object> result = new HashMap<>();
        NotificationRecipient existing = recipientMapper.findById(id);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "接收人不存在");
            return ResponseEntity.badRequest().body(result);
        }
        recipient.setId(id);
        recipientMapper.update(recipient);
        result.put("success", true);
        result.put("data", recipient);
        log.info("更新通知接收人: id={}, name={}", id, recipient.getName());
        return ResponseEntity.ok(result);
    }

    /**
     * 删除接收人
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        recipientMapper.delete(id);
        result.put("success", true);
        log.info("删除通知接收人: id={}", id);
        return ResponseEntity.ok(result);
    }

    /**
     * 启停接收人
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        NotificationRecipient existing = recipientMapper.findById(id);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "接收人不存在");
            return ResponseEntity.badRequest().body(result);
        }
        existing.setIsEnabled(!existing.getIsEnabled());
        recipientMapper.update(existing);
        result.put("success", true);
        result.put("data", existing);
        log.info("切换接收人状态: id={}, enabled={}", id, existing.getIsEnabled());
        return ResponseEntity.ok(result);
    }

    /**
     * 发送测试消息
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testAlert(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        NotificationRecipient recipient = recipientMapper.findById(id);
        if (recipient == null) {
            result.put("success", false);
            result.put("message", "接收人不存在");
            return ResponseEntity.badRequest().body(result);
        }

        if (recipient.getDingtalkUserid() == null || recipient.getDingtalkUserid().isEmpty()) {
            result.put("success", false);
            result.put("message", "接收人未配置钉钉UserID");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            boolean success = dingtalkNotificationService.sendTestMessage(recipient.getDingtalkUserid());
            if (success) {
                result.put("success", true);
                result.put("message", "测试消息发送成功");
            } else {
                result.put("success", false);
                result.put("message", "发送失败，请检查钉钉配置或UserID是否正确");
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("测试消息发送异常", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}
