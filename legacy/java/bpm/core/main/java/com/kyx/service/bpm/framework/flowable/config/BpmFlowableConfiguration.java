package com.kyx.service.bpm.framework.flowable.config;

import cn.hutool.core.collection.ListUtil;
import com.kyx.service.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
import com.kyx.service.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import com.kyx.service.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import com.kyx.service.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import com.kyx.service.business.api.user.AdminUserApi;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;

/**
 * BPM 模块的 Flowable 配置类
 *
 * @author jason
 */
@Configuration(proxyBeanMethods = false)
public class BpmFlowableConfiguration {

    @Value("${flowable.task-executor.core-pool-size:2}")
    private int flowableTaskExecutorCorePoolSize;

    @Value("${flowable.task-executor.max-pool-size:4}")
    private int flowableTaskExecutorMaxPoolSize;

    @Value("${flowable.task-executor.queue-capacity:50}")
    private int flowableTaskExecutorQueueCapacity;

    /**
     * 参考 {@link org.flowable.spring.boot.FlowableJobConfiguration} 类，创建对应的 AsyncListenableTaskExecutor Bean
     *
     * 如果不创建，会导致项目启动时，Flowable 报错的问题
     */
    @Bean(name = "applicationTaskExecutor")
    @ConditionalOnMissingBean(name = "applicationTaskExecutor")
    public AsyncListenableTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = positiveOrDefault(flowableTaskExecutorCorePoolSize, 2);
        int maxPoolSize = Math.max(corePoolSize, positiveOrDefault(flowableTaskExecutorMaxPoolSize, 4));
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(positiveOrDefault(flowableTaskExecutorQueueCapacity, 50));
        executor.setThreadNamePrefix("flowable-task-Executor-");
        executor.setAwaitTerminationSeconds(30);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }

    private static int positiveOrDefault(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    /**
     * BPM 模块的 ProcessEngineConfigurationConfigurer 实现类：
     *
     * 1. 设置各种监听器
     * 2. 设置自定义的 ActivityBehaviorFactory 实现
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> bpmProcessEngineConfigurationConfigurer(
            ObjectProvider<FlowableEventListener> listeners,
            ObjectProvider<FlowableFunctionDelegate> customFlowableFunctionDelegates,
            BpmActivityBehaviorFactory bpmActivityBehaviorFactory) {
        return configuration -> {
            // 注册监听器，例如说 BpmActivityEventListener
            configuration.setEventListeners(ListUtil.toList(listeners.iterator()));
            // 设置 ActivityBehaviorFactory 实现类，用于流程任务的审核人的自定义
            configuration.setActivityBehaviorFactory(bpmActivityBehaviorFactory);
            // 设置自定义的函数
            configuration.setCustomFlowableFunctionDelegates(ListUtil.toList(customFlowableFunctionDelegates.stream().iterator()));
        };
    }

    // =========== 审批人相关的 Bean ==========

    @Bean
    public BpmActivityBehaviorFactory bpmActivityBehaviorFactory(BpmTaskCandidateInvoker bpmTaskCandidateInvoker) {
        BpmActivityBehaviorFactory bpmActivityBehaviorFactory = new BpmActivityBehaviorFactory();
        bpmActivityBehaviorFactory.setTaskCandidateInvoker(bpmTaskCandidateInvoker);
        return bpmActivityBehaviorFactory;
    }

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // adminUserApi 可以注入成功
    public BpmTaskCandidateInvoker bpmTaskCandidateInvoker(List<BpmTaskCandidateStrategy> strategyList,
                                                           AdminUserApi adminUserApi) {
        return new BpmTaskCandidateInvoker(strategyList, adminUserApi);
    }

    // =========== 自己拓展的 Bean ==========

    @Bean
    public BpmProcessInstanceEventPublisher processInstanceEventPublisher(ApplicationEventPublisher publisher) {
        return new BpmProcessInstanceEventPublisher(publisher);
    }

}
