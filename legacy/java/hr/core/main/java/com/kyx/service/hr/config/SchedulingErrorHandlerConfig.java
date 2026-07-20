package com.kyx.service.hr.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 调度任务兜底配置，确保任务异常不会影响调度线程
 */
@Slf4j
@Configuration
public class SchedulingErrorHandlerConfig {

    @Bean(name = {"hrTaskScheduler", "taskScheduler"}, destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler hrTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("hr-schedule-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setErrorHandler(t -> log.error("[hrTaskScheduler] 定时任务执行异常", t));
        return scheduler;
    }
}
