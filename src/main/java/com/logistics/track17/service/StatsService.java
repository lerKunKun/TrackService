package com.logistics.track17.service;

import com.logistics.track17.dto.DashboardStatsResponse;
import com.logistics.track17.entity.NotificationLog;
import com.logistics.track17.entity.Shop;
import com.logistics.track17.mapper.NotificationLogMapper;
import com.logistics.track17.mapper.OrderMapper;
import com.logistics.track17.mapper.ShopMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 统计服务 - 首页店铺概览 + 告警
 */
@Slf4j
@Service
public class StatsService {

    private final ShopMapper shopMapper;
    private final OrderMapper orderMapper;
    private final NotificationLogMapper notificationLogMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsService(ShopMapper shopMapper,
            OrderMapper orderMapper,
            NotificationLogMapper notificationLogMapper) {
        this.shopMapper = shopMapper;
        this.orderMapper = orderMapper;
        this.notificationLogMapper = notificationLogMapper;
    }

    /**
     * 获取首页统计数据
     */
    public DashboardStatsResponse getDashboardStats() {
        log.info("Getting dashboard stats");

        DashboardStatsResponse response = new DashboardStatsResponse();

        // 店铺统计
        Long totalShops = shopMapper.count(null);
        List<Shop> activeShopList = shopMapper.findByIsActive(true);
        response.setTotalShops(totalShops != null ? totalShops : 0L);
        response.setActiveShops((long) activeShopList.size());

        // 全局订单统计
        Long totalOrders = orderMapper.count(null);
        response.setTotalOrders(totalOrders != null ? totalOrders : 0L);

        String today = LocalDate.now().format(DATE_FMT);
        String tomorrow = LocalDate.now().plusDays(1).format(DATE_FMT);
        Long todayOrders = orderMapper.countByDateRange(null, today, tomorrow);
        response.setTodayOrders(todayOrders != null ? todayOrders : 0L);

        // 全局销售额
        BigDecimal totalRevenue = orderMapper.sumRevenueByDateRange(null, null, null);
        response.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        BigDecimal todayRevenue = orderMapper.sumRevenueByDateRange(null, today, tomorrow);
        response.setTodayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO);

        // 店铺概览列表
        List<DashboardStatsResponse.ShopOverview> shopOverviews = new ArrayList<>();
        List<Shop> allShops = shopMapper.selectAll();
        log.info("selectAll returned {} shops, count returned {}", allShops.size(), totalShops);

        for (Shop shop : allShops) {
            try {
                DashboardStatsResponse.ShopOverview overview = new DashboardStatsResponse.ShopOverview();
                overview.setId(shop.getId());
                overview.setShopName(shop.getShopName());
                overview.setShopDomain(shop.getShopDomain());
                overview.setPlatform(shop.getPlatform());
                overview.setPlanDisplayName(shop.getPlanDisplayName());
                overview.setCurrency(shop.getCurrency());
                overview.setConnectionStatus(shop.getConnectionStatus());
                overview.setIsActive(shop.getIsActive());

                Long shopOrderCount = orderMapper.count(shop.getId());
                overview.setOrderCount(shopOrderCount != null ? shopOrderCount : 0L);

                BigDecimal shopRevenue = orderMapper.sumRevenueByDateRange(shop.getId(), null, null);
                overview.setRevenue(shopRevenue != null ? shopRevenue : BigDecimal.ZERO);

                if (shop.getLastSyncTime() != null) {
                    overview.setLastSyncTime(shop.getLastSyncTime().format(DATETIME_FMT));
                }

                shopOverviews.add(overview);
            } catch (Exception e) {
                log.error("Error building shop overview for shopId={}: {}", shop.getId(), e.getMessage(), e);
            }
        }
        response.setShops(shopOverviews);

        // 最近告警
        List<NotificationLog> recentLogs = notificationLogMapper.findRecent(0, 20);
        List<DashboardStatsResponse.AlertItem> alertItems = new ArrayList<>();
        long pendingCount = 0;

        for (NotificationLog logEntry : recentLogs) {
            DashboardStatsResponse.AlertItem item = new DashboardStatsResponse.AlertItem();
            item.setId(logEntry.getId());
            item.setAlertType(logEntry.getAlertType());
            item.setSeverity(logEntry.getSeverity());
            item.setTitle(logEntry.getTitle());
            item.setContent(logEntry.getContent());
            item.setSource(logEntry.getSource());
            item.setSendStatus(logEntry.getSendStatus());
            item.setShopId(logEntry.getShopId());

            if (logEntry.getCreatedTime() != null) {
                item.setCreatedTime(logEntry.getCreatedTime().format(DATETIME_FMT));
            }

            // 关联店铺名
            if (logEntry.getShopId() != null) {
                Shop shop = shopMapper.selectById(logEntry.getShopId());
                if (shop != null) {
                    item.setShopName(shop.getShopName() != null ? shop.getShopName() : shop.getShopDomain());
                }
            }

            alertItems.add(item);

            if ("PENDING".equals(logEntry.getSendStatus()) || "FAILED".equals(logEntry.getSendStatus())) {
                pendingCount++;
            }
        }
        response.setRecentAlerts(alertItems);
        response.setPendingAlerts(pendingCount);

        log.info("Dashboard stats retrieved successfully");
        return response;
    }
}
