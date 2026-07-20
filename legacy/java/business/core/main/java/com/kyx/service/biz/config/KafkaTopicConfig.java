package com.kyx.service.biz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Business 服务 Kafka 主题配置
 */
@Slf4j
@Configuration
public class KafkaTopicConfig implements CommandLineRunner {

    /**
     * BPM 流程状态变更主题
     */
    public static final String BPM_PROCESS_STATUS_TOPIC = "bpm.process.status.changed";

    @Override
    public void run(String... args) {
        log.info("Business 服务 Kafka 配置已准备，监听主题：{}", BPM_PROCESS_STATUS_TOPIC);
    }

}
