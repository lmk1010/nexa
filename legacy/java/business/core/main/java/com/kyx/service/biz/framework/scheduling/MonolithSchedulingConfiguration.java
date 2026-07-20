package com.kyx.service.biz.framework.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 单体统一调度开关。默认保持原服务行为，启动诊断或维护时可通过
 * {@code kyx.monolith.scheduling.enabled=false} 禁止所有 @Scheduled 任务。
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "kyx.monolith.scheduling", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class MonolithSchedulingConfiguration {
}
