package com.logistics.track17.service;

import com.logistics.track17.entity.EmailMonitorConfig;
import com.logistics.track17.enums.AlertType;
import com.logistics.track17.mapper.EmailMonitorConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.AndTerm;
import javax.mail.search.SearchTerm;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * 邮箱监控服务
 * 定时检查配置的邮箱，解析Shopify通知邮件，触发告警
 */
@Service
@Slf4j
public class EmailMonitorService {

    private final EmailMonitorConfigMapper configMapper;
    private final DingtalkNotificationService notificationService;

    // Shopify告警邮件关键词匹配
    private static final Pattern IP_INFRINGEMENT_PATTERN = Pattern
            .compile("(?i)(intellectual property|infringement|DMCA|counterfeit|copyright|trademark)");
    private static final Pattern ACCOUNT_RESTRICTED_PATTERN = Pattern
            .compile("(?i)(account.*(?:restricted|suspended|deactivated|frozen|closed)|your store has been)");
    private static final Pattern PAYMENT_HOLD_PATTERN = Pattern
            .compile("(?i)(payment.*(?:hold|reserve|withheld|suspended)|payout.*(?:hold|delayed|paused))");
    private static final Pattern DISPUTE_EMAIL_PATTERN = Pattern.compile("(?i)(chargeback|dispute|inquiry.*payment)");

    public EmailMonitorService(EmailMonitorConfigMapper configMapper,
            DingtalkNotificationService notificationService) {
        this.configMapper = configMapper;
        this.notificationService = notificationService;
    }

    /**
     * 定时检查所有已启用的邮箱 (每5分钟)
     */
    @Scheduled(fixedDelayString = "${email.monitor.interval:300000}", initialDelay = 60000)
    public void checkAllMailboxes() {
        List<EmailMonitorConfig> enabledConfigs = configMapper.findAllEnabled();
        if (enabledConfigs.isEmpty()) {
            return;
        }

        log.info("开始邮箱监控检查，共{}个邮箱", enabledConfigs.size());

        for (EmailMonitorConfig config : enabledConfigs) {
            try {
                checkMailbox(config);
            } catch (Exception e) {
                log.error("邮箱检查失败: name={}, host={}", config.getName(), config.getHost(), e);
                configMapper.updateCheckStatus(config.getId(), LocalDateTime.now(), "FAILED", e.getMessage());
            }
        }
    }

    /**
     * 检查单个邮箱
     */
    public void checkMailbox(EmailMonitorConfig config) {
        Store store = null;
        Folder inbox = null;

        try {
            Properties props = new Properties();
            props.setProperty("mail.store.protocol", config.getProtocol());
            props.setProperty("mail." + config.getProtocol() + ".host", config.getHost());
            props.setProperty("mail." + config.getProtocol() + ".port", String.valueOf(config.getPort()));
            props.setProperty("mail." + config.getProtocol() + ".ssl.enable", "true");
            props.setProperty("mail." + config.getProtocol() + ".connectiontimeout", "15000");
            props.setProperty("mail." + config.getProtocol() + ".timeout", "15000");

            Session session = Session.getInstance(props);
            store = session.getStore(config.getProtocol());
            store.connect(config.getHost(), config.getPort(), config.getUsername(), config.getPassword());

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // 搜索条件：从上次检查时间起，且发件人匹配
            SearchTerm searchTerm = buildSearchTerm(config);
            Message[] messages = inbox.search(searchTerm);

            int alertCount = 0;
            for (Message message : messages) {
                try {
                    String subject = message.getSubject();
                    String body = extractTextContent(message);

                    AlertType detectedType = classifyEmail(subject, body);
                    if (detectedType != null) {
                        String summary = truncate(body, 500);
                        String msgId = message.getHeader("Message-ID") != null
                                ? message.getHeader("Message-ID")[0]
                                : String.valueOf(message.getReceivedDate().getTime());

                        String content = notificationService.formatEmailAlertMessage(
                                null, detectedType, subject, summary);
                        notificationService.sendAlert(detectedType,
                                detectedType.getDescription() + " (邮件检测)",
                                content, null, "EMAIL",
                                "EMAIL:" + config.getId() + ":" + msgId);
                        alertCount++;
                    }
                } catch (Exception msgEx) {
                    log.warn("解析邮件失败: subject={}", message.getSubject(), msgEx);
                }
            }

            // 更新检查状态
            configMapper.updateCheckStatus(config.getId(), LocalDateTime.now(), "SUCCESS", null);
            if (alertCount > 0) {
                log.info("邮箱 {} 发现 {} 条告警邮件", config.getName(), alertCount);
            }

        } catch (Exception e) {
            throw new RuntimeException("邮箱连接/检查失败: " + e.getMessage(), e);
        } finally {
            try {
                if (inbox != null && inbox.isOpen())
                    inbox.close(false);
                if (store != null && store.isConnected())
                    store.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 构建搜索条件
     */
    private SearchTerm buildSearchTerm(EmailMonitorConfig config) {
        // 时间条件：只查上次检查后的邮件
        Date since;
        if (config.getLastCheckTime() != null) {
            since = Date.from(config.getLastCheckTime().atZone(ZoneId.systemDefault()).toInstant());
        } else {
            // 首次：只查过去24小时
            since = Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant());
        }
        SearchTerm dateTerm = new ReceivedDateTerm(ComparisonTerm.GE, since);

        // 发件人过滤
        if (config.getSenderFilter() != null && !config.getSenderFilter().isEmpty()) {
            SearchTerm fromTerm = new FromStringTerm(config.getSenderFilter());
            return new AndTerm(dateTerm, fromTerm);
        }

        return dateTerm;
    }

    /**
     * 分类邮件告警类型
     */
    private AlertType classifyEmail(String subject, String body) {
        String text = (subject != null ? subject : "") + " " + (body != null ? body : "");

        if (IP_INFRINGEMENT_PATTERN.matcher(text).find()) {
            return AlertType.INFRINGEMENT;
        }
        if (ACCOUNT_RESTRICTED_PATTERN.matcher(text).find()) {
            return AlertType.ACCOUNT_RESTRICTED;
        }
        if (PAYMENT_HOLD_PATTERN.matcher(text).find()) {
            return AlertType.PAYMENT_HOLD;
        }
        if (DISPUTE_EMAIL_PATTERN.matcher(text).find()) {
            return AlertType.DISPUTE;
        }
        return null;
    }

    /**
     * 提取邮件文本内容
     */
    private String extractTextContent(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                    sb.append(part.getContent().toString());
                }
            }
            return sb.toString();
        }
        return content != null ? content.toString() : "";
    }

    private String truncate(String text, int maxLen) {
        if (text == null)
            return "";
        // 去除HTML标签
        text = text.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
