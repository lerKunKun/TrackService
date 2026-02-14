package com.logistics.track17.service;

import com.logistics.track17.entity.Shop;
import com.logistics.track17.enums.AlertType;
import com.logistics.track17.mapper.OrderMapper;
import com.logistics.track17.mapper.ShopMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 数据汇总服务
 * 生成日报/月报/季报/年报，发送钉钉通知
 */
@Service
@Slf4j
public class DataSummaryService {

    private final OrderMapper orderMapper;
    private final ShopMapper shopMapper;
    private final DingtalkNotificationService notificationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DataSummaryService(OrderMapper orderMapper,
            ShopMapper shopMapper,
            DingtalkNotificationService notificationService) {
        this.orderMapper = orderMapper;
        this.shopMapper = shopMapper;
        this.notificationService = notificationService;
    }

    /**
     * 生成并发送日报
     */
    public void sendDailySummary() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String startDate = yesterday.format(DATE_FMT);
        String endDate = LocalDate.now().format(DATE_FMT);

        String title = "📊 日报 - " + yesterday.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String content = buildSummaryContent("日报", startDate, endDate, yesterday.toString());
        String dedupKey = "DAILY_SUMMARY:" + yesterday;

        notificationService.sendAlert(AlertType.DAILY_SUMMARY, title, content, null, "SCHEDULED", dedupKey);
        log.info("日报已发送: {}", yesterday);
    }

    /**
     * 生成并发送月报
     */
    public void sendMonthlySummary() {
        LocalDate firstDayLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate firstDayThisMonth = LocalDate.now().withDayOfMonth(1);
        String startDate = firstDayLastMonth.format(DATE_FMT);
        String endDate = firstDayThisMonth.format(DATE_FMT);

        String monthLabel = firstDayLastMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月"));
        String title = "📊 月报 - " + monthLabel;
        String content = buildSummaryContent("月报", startDate, endDate, monthLabel);
        String dedupKey = "MONTHLY_SUMMARY:" + firstDayLastMonth;

        notificationService.sendAlert(AlertType.MONTHLY_SUMMARY, title, content, null, "SCHEDULED", dedupKey);
        log.info("月报已发送: {}", monthLabel);
    }

    /**
     * 生成并发送季报
     */
    public void sendQuarterlySummary() {
        LocalDate now = LocalDate.now();
        int lastQuarter = (now.getMonthValue() - 1) / 3;
        if (lastQuarter == 0)
            lastQuarter = 4;
        int year = lastQuarter == 4 ? now.getYear() - 1 : now.getYear();

        LocalDate quarterStart = LocalDate.of(year, (lastQuarter - 1) * 3 + 1, 1);
        LocalDate quarterEnd = quarterStart.plusMonths(3);
        String startDate = quarterStart.format(DATE_FMT);
        String endDate = quarterEnd.format(DATE_FMT);

        String quarterLabel = year + "年Q" + lastQuarter;
        String title = "📊 季报 - " + quarterLabel;
        String content = buildSummaryContent("季报", startDate, endDate, quarterLabel);
        String dedupKey = "QUARTERLY_SUMMARY:" + quarterLabel;

        notificationService.sendAlert(AlertType.QUARTERLY_SUMMARY, title, content, null, "SCHEDULED", dedupKey);
        log.info("季报已发送: {}", quarterLabel);
    }

    /**
     * 生成并发送年报
     */
    public void sendYearlySummary() {
        int lastYear = LocalDate.now().getYear() - 1;
        LocalDate yearStart = LocalDate.of(lastYear, 1, 1);
        LocalDate yearEnd = LocalDate.of(lastYear + 1, 1, 1);
        String startDate = yearStart.format(DATE_FMT);
        String endDate = yearEnd.format(DATE_FMT);

        String yearLabel = lastYear + "年";
        String title = "📊 年报 - " + yearLabel;
        String content = buildSummaryContent("年报", startDate, endDate, yearLabel);
        String dedupKey = "YEARLY_SUMMARY:" + lastYear;

        notificationService.sendAlert(AlertType.YEARLY_SUMMARY, title, content, null, "SCHEDULED", dedupKey);
        log.info("年报已发送: {}", yearLabel);
    }

    /**
     * 构建汇总内容
     */
    private String buildSummaryContent(String reportType, String startDate, String endDate, String periodLabel) {
        List<Shop> activeShops = shopMapper.findByIsActive(true);

        StringBuilder sb = new StringBuilder();
        sb.append("## 📊 ").append(reportType).append(" - ").append(periodLabel).append("\n\n");
        sb.append("**统计范围**: ").append(startDate).append(" ~ ").append(endDate).append("\n\n");

        // 全局汇总
        Long totalOrders = orderMapper.countByDateRange(null, startDate, endDate);
        BigDecimal totalRevenue = orderMapper.sumRevenueByDateRange(null, startDate, endDate);

        sb.append("### 📈 全局汇总\n\n");
        sb.append("| 指标 | 数值 |\n");
        sb.append("|------|------|\n");
        sb.append("| 总订单数 | ").append(totalOrders != null ? totalOrders : 0).append(" |\n");
        sb.append("| 总销售额 | ").append(formatAmount(totalRevenue)).append(" |\n");
        sb.append("| 活跃店铺 | ").append(activeShops.size()).append(" |\n\n");

        // 分店铺汇总
        if (!activeShops.isEmpty()) {
            sb.append("### 🏪 分店铺数据\n\n");
            sb.append("| 店铺 | 订单数 | 销售额 |\n");
            sb.append("|------|--------|--------|\n");

            for (Shop shop : activeShops) {
                Long shopOrders = orderMapper.countByDateRange(shop.getId(), startDate, endDate);
                BigDecimal shopRevenue = orderMapper.sumRevenueByDateRange(shop.getId(), startDate, endDate);

                String shopName = shop.getShopName() != null ? shop.getShopName() : shop.getShopDomain();
                sb.append("| ").append(shopName).append(" | ");
                sb.append(shopOrders != null ? shopOrders : 0).append(" | ");
                sb.append(formatAmount(shopRevenue)).append(" |\n");
            }
            sb.append("\n");
        }

        sb.append("> 数据来源：系统同步订单，实际数据以Shopify后台为准。\n");
        return sb.toString();
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "$0.00";
        }
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
