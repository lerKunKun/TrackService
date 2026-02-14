package com.logistics.track17.controller;

import com.logistics.track17.entity.EmailMonitorConfig;
import com.logistics.track17.mapper.EmailMonitorConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.mail.Session;
import javax.mail.Store;
import java.util.*;

/**
 * 邮箱监控配置 Controller
 * 前端页面管理监控邮箱（支持多邮箱）
 */
@Slf4j
@RestController
@RequestMapping("/alert-config/email-monitors")
public class EmailMonitorConfigController {

    private final EmailMonitorConfigMapper configMapper;

    public EmailMonitorConfigController(EmailMonitorConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    /**
     * 获取所有邮箱配置
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        List<EmailMonitorConfig> configs = configMapper.findAll();
        // 隐藏密码
        configs.forEach(c -> c.setPassword("******"));
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", configs);
        return ResponseEntity.ok(result);
    }

    /**
     * 添加邮箱配置
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody EmailMonitorConfig config) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (config.getIsEnabled() == null) {
                config.setIsEnabled(true);
            }
            if (config.getPort() == null) {
                config.setPort(993);
            }
            if (config.getProtocol() == null || config.getProtocol().isEmpty()) {
                config.setProtocol("imaps");
            }
            if (config.getCheckInterval() == null) {
                config.setCheckInterval(300);
            }
            if (config.getSenderFilter() == null || config.getSenderFilter().isEmpty()) {
                config.setSenderFilter("noreply@shopify.com");
            }
            configMapper.insert(config);
            config.setPassword("******");
            result.put("success", true);
            result.put("data", config);
            log.info("添加邮箱监控配置: {} ({})", config.getName(), config.getUsername());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("添加邮箱监控配置失败", e);
            result.put("success", false);
            result.put("message", "添加失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 更新邮箱配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
            @RequestBody EmailMonitorConfig config) {
        Map<String, Object> result = new HashMap<>();
        EmailMonitorConfig existing = configMapper.findById(id);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "配置不存在");
            return ResponseEntity.badRequest().body(result);
        }
        config.setId(id);
        // 如果密码为掩码值，保留原密码
        if ("******".equals(config.getPassword())) {
            config.setPassword(existing.getPassword());
        }
        configMapper.update(config);
        config.setPassword("******");
        result.put("success", true);
        result.put("data", config);
        log.info("更新邮箱监控配置: id={}, name={}", id, config.getName());
        return ResponseEntity.ok(result);
    }

    /**
     * 删除邮箱配置
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        configMapper.delete(id);
        result.put("success", true);
        log.info("删除邮箱监控配置: id={}", id);
        return ResponseEntity.ok(result);
    }

    /**
     * 启停邮箱监控
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        EmailMonitorConfig existing = configMapper.findById(id);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "配置不存在");
            return ResponseEntity.badRequest().body(result);
        }
        existing.setIsEnabled(!existing.getIsEnabled());
        configMapper.update(existing);
        existing.setPassword("******");
        result.put("success", true);
        result.put("data", existing);
        log.info("切换邮箱监控状态: id={}, enabled={}", id, existing.getIsEnabled());
        return ResponseEntity.ok(result);
    }

    /**
     * 测试邮箱连接
     */
    @PostMapping("/{id}/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        EmailMonitorConfig config = configMapper.findById(id);
        if (config == null) {
            result.put("success", false);
            result.put("message", "配置不存在");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            Properties props = new Properties();
            props.setProperty("mail.store.protocol", config.getProtocol());
            props.setProperty("mail." + config.getProtocol() + ".host", config.getHost());
            props.setProperty("mail." + config.getProtocol() + ".port", String.valueOf(config.getPort()));
            props.setProperty("mail." + config.getProtocol() + ".ssl.enable", "true");
            props.setProperty("mail." + config.getProtocol() + ".connectiontimeout", "10000");
            props.setProperty("mail." + config.getProtocol() + ".timeout", "10000");

            Session session = Session.getInstance(props);
            Store store = session.getStore(config.getProtocol());
            store.connect(config.getHost(), config.getPort(), config.getUsername(), config.getPassword());
            store.close();

            // 更新连接状态
            configMapper.updateCheckStatus(id, java.time.LocalDateTime.now(), "SUCCESS", null);

            result.put("success", true);
            result.put("message", "连接成功");
            log.info("邮箱连接测试成功: id={}, host={}", id, config.getHost());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            configMapper.updateCheckStatus(id, java.time.LocalDateTime.now(), "FAILED", errorMsg);

            result.put("success", false);
            result.put("message", "连接失败: " + errorMsg);
            log.error("邮箱连接测试失败: id={}, host={}", id, config.getHost(), e);
            return ResponseEntity.ok(result);
        }
    }
}
