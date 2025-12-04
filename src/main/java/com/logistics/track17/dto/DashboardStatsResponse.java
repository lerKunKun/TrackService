package com.logistics.track17.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 首页统计数据响应
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardStatsResponse {

    /**
     * 运单总数
     */
    private Long totalTrackings;

    /**
     * 店铺总数
     */
    private Long totalShops;

    /**
     * 用户总数
     */
    private Long totalUsers;

    /**
     * 今日新增运单数
     */
    private Long todayTrackings;

    /**
     * 各状态运单数量
     */
    private StatusStats statusStats;

    /**
     * 各承运商运单数量（前10）
     */
    private List<CarrierStats> carrierStats;

    /**
     * 最近7天运单趋势
     */
    private List<DailyStats> dailyStats;

    @Data
    public static class StatusStats {
        private Long infoReceived;
        private Long inTransit;
        private Long delivered;
        private Long exception;
    }

    @Data
    public static class CarrierStats {
        private String carrierCode;
        private Long count;
    }

    @Data
    public static class DailyStats {
        private String date;
        private Long count;
    }
}
