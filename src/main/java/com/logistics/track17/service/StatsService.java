package com.logistics.track17.service;

import com.logistics.track17.dto.DashboardStatsResponse;
import com.logistics.track17.mapper.ShopMapper;
import com.logistics.track17.mapper.TrackingNumberMapper;
import com.logistics.track17.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 统计服务
 */
@Slf4j
@Service
public class StatsService {

    private final TrackingNumberMapper trackingNumberMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;

    public StatsService(TrackingNumberMapper trackingNumberMapper,
            ShopMapper shopMapper,
            UserMapper userMapper) {
        this.trackingNumberMapper = trackingNumberMapper;
        this.shopMapper = shopMapper;
        this.userMapper = userMapper;
    }

    /**
     * 获取首页统计数据
     */
    public DashboardStatsResponse getDashboardStats() {
        log.info("Getting dashboard stats");

        DashboardStatsResponse response = new DashboardStatsResponse();

        // 基础统计
        response.setTotalTrackings(trackingNumberMapper.count(null, null, null, null, null, null));
        response.setTotalShops(shopMapper.count(null));
        response.setTotalUsers(userMapper.count());
        response.setTodayTrackings(trackingNumberMapper.countToday());

        // 按状态统计
        DashboardStatsResponse.StatusStats statusStats = new DashboardStatsResponse.StatusStats();
        statusStats.setInfoReceived(trackingNumberMapper.countByStatus("InfoReceived"));
        statusStats.setInTransit(trackingNumberMapper.countByStatus("InTransit"));
        statusStats.setDelivered(trackingNumberMapper.countByStatus("Delivered"));
        statusStats.setException(trackingNumberMapper.countByStatus("Exception"));
        response.setStatusStats(statusStats);

        // 按承运商统计（前10）
        List<Map<String, Object>> carrierData = trackingNumberMapper.countByCarrier(10);
        List<DashboardStatsResponse.CarrierStats> carrierStats = new ArrayList<>();
        for (Map<String, Object> item : carrierData) {
            DashboardStatsResponse.CarrierStats stat = new DashboardStatsResponse.CarrierStats();
            stat.setCarrierCode((String) item.get("carrierCode"));
            stat.setCount(((Number) item.get("count")).longValue());
            carrierStats.add(stat);
        }
        response.setCarrierStats(carrierStats);

        // 最近7天趋势
        List<Map<String, Object>> dailyData = trackingNumberMapper.countByDay(7);
        List<DashboardStatsResponse.DailyStats> dailyStats = new ArrayList<>();

        // 填充缺失的日期
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(formatter);

            DashboardStatsResponse.DailyStats stat = new DashboardStatsResponse.DailyStats();
            stat.setDate(dateStr);

            // 查找对应日期的数据
            long count = 0;
            for (Map<String, Object> item : dailyData) {
                Object dateObj = item.get("date");
                String itemDateStr;
                if (dateObj instanceof java.sql.Date) {
                    itemDateStr = ((java.sql.Date) dateObj).toLocalDate().format(formatter);
                } else if (dateObj instanceof LocalDate) {
                    itemDateStr = ((LocalDate) dateObj).format(formatter);
                } else {
                    itemDateStr = dateObj.toString();
                }

                if (dateStr.equals(itemDateStr)) {
                    count = ((Number) item.get("count")).longValue();
                    break;
                }
            }
            stat.setCount(count);
            dailyStats.add(stat);
        }
        response.setDailyStats(dailyStats);

        log.info("Dashboard stats retrieved successfully");
        return response;
    }
}
