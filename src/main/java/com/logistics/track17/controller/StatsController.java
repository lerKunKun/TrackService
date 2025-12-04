package com.logistics.track17.controller;

import com.logistics.track17.dto.DashboardStatsResponse;
import com.logistics.track17.dto.Result;
import com.logistics.track17.service.StatsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * 获取首页统计数据
     */
    @GetMapping("/dashboard")
    public Result<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse response = statsService.getDashboardStats();
        return Result.success(response);
    }
}
