package com.logistics.track17.service;

import com.logistics.track17.entity.NotificationLog;
import com.logistics.track17.entity.NotificationRecipient;
import com.logistics.track17.enums.AlertType;
import com.logistics.track17.mapper.NotificationLogMapper;
import com.logistics.track17.mapper.NotificationRecipientMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 钉钉通知统一服务
 * 负责消息格式化、接收人分发、日志记录
 */
@Service
@Slf4j
public class DingtalkNotificationService {

    private final DingtalkApiService dingtalkApiService;
    private final NotificationLogMapper notificationLogMapper;
    private final NotificationRecipientMapper recipientMapper;

    public DingtalkNotificationService(DingtalkApiService dingtalkApiService,
            NotificationLogMapper notificationLogMapper,
            NotificationRecipientMapper recipientMapper) {
        this.dingtalkApiService = dingtalkApiService;
        this.notificationLogMapper = notificationLogMapper;
        this.recipientMapper = recipientMapper;
    }

    /**
     * 发送Alert通知给所有订阅该类型的接收人
     *
     * @param alertType Alert类型
     * @param title     标题
     * @param content   Markdown内容
     * @param shopId    店铺ID（可为null）
     * @param source    数据源: WEBHOOK/API_POLL/EMAIL
     * @param dedupKey  去重键（可为null）
     */
    public void sendAlert(AlertType alertType, String title, String content,
            Long shopId, String source, String dedupKey) {
        // 1. 去重检查
        if (dedupKey != null && notificationLogMapper.countByDedupKey(dedupKey) > 0) {
            log.info("通知已发送过，跳过去重: dedupKey={}", dedupKey);
            return;
        }

        // 2. 获取订阅了该Alert类型的接收人
        List<NotificationRecipient> recipients = recipientMapper.findAllEnabled();
        List<NotificationRecipient> subscribedRecipients = recipients.stream()
                .filter(r -> r.isSubscribed(alertType.getCode()))
                .collect(Collectors.toList());

        if (subscribedRecipients.isEmpty()) {
            log.warn("没有找到订阅 {} 类型通知的接收人", alertType.getCode());
            return;
        }

        // 3. 逐个发送并记录日志
        for (NotificationRecipient recipient : subscribedRecipients) {
            NotificationLog logEntry = new NotificationLog();
            logEntry.setShopId(shopId);
            logEntry.setAlertType(alertType.getCode());
            logEntry.setSeverity(alertType.getDefaultSeverity());
            logEntry.setSource(source);
            logEntry.setTitle(title);
            logEntry.setContent(content);
            logEntry.setDedupKey(dedupKey != null ? dedupKey + ":" + recipient.getDingtalkUserid() : null);
            logEntry.setRecipientUserid(recipient.getDingtalkUserid());
            logEntry.setSendStatus("PENDING");

            try {
                // 插入待发送日志
                notificationLogMapper.insert(logEntry);

                // 发送工作通知
                boolean success = dingtalkApiService.sendWorkNotification(
                        recipient.getDingtalkUserid(), title, content);

                // 更新状态
                logEntry.setSendStatus(success ? "SUCCESS" : "FAILED");
                logEntry.setSentTime(LocalDateTime.now());
                if (!success) {
                    logEntry.setErrorMessage("钉钉API返回失败");
                }
                notificationLogMapper.updateStatus(logEntry);

                log.info("通知发送{}: type={}, 接收人={}", success ? "成功" : "失败",
                        alertType.getCode(), recipient.getName());

            } catch (Exception e) {
                log.error("通知发送异常: type={}, 接收人={}", alertType.getCode(),
                        recipient.getName(), e);
                logEntry.setSendStatus("FAILED");
                logEntry.setErrorMessage(e.getMessage());
                logEntry.setSentTime(LocalDateTime.now());
                try {
                    notificationLogMapper.updateStatus(logEntry);
                } catch (Exception ex) {
                    log.error("更新通知日志失败", ex);
                }
            }
        }
    }

    // ======================== 消息格式化方法 ========================

    /**
     * 格式化争议(Chargeback)通知
     */
    public String formatDisputeMessage(String shopName, String amount, String currency,
            String reason, String type, String status,
            String orderId, String evidenceDueBy) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 🚨 ").append(shopName).append(" - 收到").append(type).append("争议\n\n");
        sb.append("**金额**: ").append(amount).append(" ").append(currency).append("\n\n");
        sb.append("**原因**: ").append(translateDisputeReason(reason)).append("\n\n");
        if (orderId != null && !orderId.isEmpty()) {
            sb.append("**关联订单**: #").append(orderId).append("\n\n");
        }
        sb.append("**状态**: ").append(status).append("\n\n");
        if (evidenceDueBy != null && !evidenceDueBy.isEmpty()) {
            sb.append("**证据截止**: ").append(evidenceDueBy).append("\n\n");
            sb.append("> ⚠️ 请在截止日前提交争议证据！\n");
        }
        return sb.toString();
    }

    /**
     * 格式化App卸载通知
     */
    public String formatAppUninstalledMessage(String shopName, String shopDomain) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ⚠️ 应用被卸载\n\n");
        sb.append("**店铺**: ").append(shopName != null ? shopName : shopDomain).append("\n\n");
        sb.append("**域名**: ").append(shopDomain).append("\n\n");
        sb.append("**时间**: ").append(LocalDateTime.now()).append("\n\n");
        sb.append("> 请确认是否需要重新授权安装。\n");
        return sb.toString();
    }

    /**
     * 格式化邮件Alert通知
     */
    public String formatEmailAlertMessage(String shopName, AlertType alertType,
            String emailSubject, String emailSummary) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 🚨 ").append(alertType.getDescription()).append("\n\n");
        if (shopName != null) {
            sb.append("**店铺**: ").append(shopName).append("\n\n");
        }
        sb.append("**检测方式**: Shopify邮件通知\n\n");
        sb.append("**邮件标题**: ").append(emailSubject).append("\n\n");
        sb.append("**邮件摘要**:\n\n").append(emailSummary).append("\n\n");
        sb.append("> ⚠️ 请立即登录Shopify后台处理！\n");
        return sb.toString();
    }

    /**
     * 发送测试消息
     */
    public boolean sendTestMessage(String dingUserId) {
        String title = "🔔 测试通知";
        StringBuilder content = new StringBuilder();
        content.append("## 🔔 测试通知\n\n");
        content.append("这是一条来自 BIOU EMP SYSTEM 的测试消息\n\n");
        content.append("**发送时间**: ").append(LocalDateTime.now()).append("\n\n");
        content.append("**接收人**: ").append(dingUserId).append("\n\n");
        content.append("> 如果您收到此消息，说明钉钉通知配置正确。 ✅\n");
//        content.append("![Png](https://free.wzznft.com/i/2026/03/11/u8ednk.png)\n\n");
//        content.append("> 15min 下班👻👻👻\n");

        try {
            return dingtalkApiService.sendWorkNotification(dingUserId, title, content.toString());
        } catch (Exception e) {
            log.error("发送测试消息失败: {}", dingUserId, e);
            throw new RuntimeException("发送测试消息失败: " + e.getMessage());
        }
    }

    /**
     * 翻译争议原因
     */
    private String translateDisputeReason(String reason) {
        if (reason == null)
            return "未知";
        switch (reason.toLowerCase()) {
            case "fraudulent":
                return "欺诈交易 (fraudulent)";
            case "credit_not_processed":
                return "退款未处理 (credit_not_processed)";
            case "duplicate":
                return "重复扣款 (duplicate)";
            case "subscription_canceled":
                return "订阅已取消 (subscription_canceled)";
            case "product_not_received":
                return "商品未收到 (product_not_received)";
            case "unrecognized":
                return "未识别交易 (unrecognized)";
            case "general":
                return "一般争议 (general)";
            default:
                return reason;
        }
    }
}
