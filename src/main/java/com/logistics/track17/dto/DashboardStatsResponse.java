package com.logistics.track17.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页统计数据响应 - 店铺概览 + 告警
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardStatsResponse {

    /**
     * 店铺总数
     */
    private Long totalShops;

    /**
     * 活跃店铺数
     */
    private Long activeShops;

    /**
     * 总订单数
     */
    private Long totalOrders;

    /**
     * 今日订单数
     */
    private Long todayOrders;

    /**
     * 总销售额
     */
    private BigDecimal totalRevenue;

    /**
     * 今日销售额
     */
    private BigDecimal todayRevenue;

    /**
     * 未处理告警数
     */
    private Long pendingAlerts;

    /**
     * 店铺列表概览
     */
    private List<ShopOverview> shops;

    /**
     * 最近告警列表
     */
    private List<AlertItem> recentAlerts;

    /**
     * 店铺概览
     */
    @Data
    public static class ShopOverview {
        private Long id;
        private String shopName;
        private String shopDomain;
        private String platform;
        private String planDisplayName;
        private String currency;
        private String connectionStatus;
        private Boolean isActive;
        private Long orderCount;
        private BigDecimal revenue;
        private String lastSyncTime;
    }

    /**
     * 告警项
     */
    @Data
    public static class AlertItem {
        private Long id;
        private String alertType;
        private String severity;
        private String title;
        private String content;
        private String source;
        private String sendStatus;
        private String createdTime;
        private Long shopId;
        private String shopName;
    }
}
