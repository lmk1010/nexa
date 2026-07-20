package com.kyx.service.bpm.framework.flowable.core.event;

import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.bpm.config.KafkaTopicConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Kafka 的 BPM 流程实例状态事件发布器。
 */
@Slf4j
@AllArgsConstructor
@Component
public class BpmProcessInstanceKafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 发送流程实例状态变更事件到 Kafka。
     * Kafka 不可达时降级为警告，避免阻断同 JVM 的本地 Spring 事件回写。
     */
    public void sendProcessInstanceStatusEvent(@Valid BpmProcessInstanceStatusKafkaEvent event) {
        try {
            log.info("发送BPM流程状态变更事件到Kafka: processDefinitionKey={}, businessKey={}, status={}",
                    event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus());
            // 使用 businessKey 作为消息 key，确保同一业务的消息发送到同一分区，保证顺序
            kafkaTemplate.send(KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC, event.getBusinessKey(), event)
                    .get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 模块化单体下本地 ApplicationEvent 已可回写业务状态；Kafka 失败不应拖垮流程完成回调。
            log.warn("发送BPM流程状态变更事件到Kafka失败（已忽略，依赖本地事件/后续重试）: processDefinitionKey={}, businessKey={}, status={}, err={}",
                    event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus(), e.toString());
        }
    }
}
