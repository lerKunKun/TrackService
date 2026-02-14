package com.logistics.track17.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 异步 + 定时任务配置
 * 用于通知推送和定时汇总等异步操作
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // 使用Spring默认的SimpleAsyncTaskExecutor
    // 如有需要可自定义ThreadPoolTaskExecutor
}
