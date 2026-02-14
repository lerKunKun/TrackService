package com.logistics.track17.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 数据汇总定时调度器
 * 触发日报/月报/季报/年报的生成和发送
 */
@Service
@Slf4j
public class DataSummaryScheduler {

    private final DataSummaryService dataSummaryService;

    public DataSummaryScheduler(DataSummaryService dataSummaryService) {
        this.dataSummaryService = dataSummaryService;
    }

    /**
     * 日报：每天上午9点发送前一天数据
     * cron: 秒 分 时 日 月 星期
     */
    @Scheduled(cron = "${data.summary.daily.cron:0 0 9 * * ?}")
    public void triggerDailySummary() {
        log.info("触发日报生成...");
        try {
            dataSummaryService.sendDailySummary();
        } catch (Exception e) {
            log.error("日报生成失败", e);
        }
    }

    /**
     * 月报：每月1日上午10点发送上月数据
     */
    @Scheduled(cron = "${data.summary.monthly.cron:0 0 10 1 * ?}")
    public void triggerMonthlySummary() {
        log.info("触发月报生成...");
        try {
            dataSummaryService.sendMonthlySummary();
        } catch (Exception e) {
            log.error("月报生成失败", e);
        }
    }

    /**
     * 季报：每季度第一天（1/1, 4/1, 7/1, 10/1）上午10点
     */
    @Scheduled(cron = "${data.summary.quarterly.cron:0 0 10 1 1,4,7,10 ?}")
    public void triggerQuarterlySummary() {
        log.info("触发季报生成...");
        try {
            dataSummaryService.sendQuarterlySummary();
        } catch (Exception e) {
            log.error("季报生成失败", e);
        }
    }

    /**
     * 年报：每年1月1日上午11点发送上年数据
     */
    @Scheduled(cron = "${data.summary.yearly.cron:0 0 11 1 1 ?}")
    public void triggerYearlySummary() {
        log.info("触发年报生成...");
        try {
            dataSummaryService.sendYearlySummary();
        } catch (Exception e) {
            log.error("年报生成失败", e);
        }
    }
}
