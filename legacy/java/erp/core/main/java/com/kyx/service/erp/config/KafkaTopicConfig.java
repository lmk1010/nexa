package com.kyx.service.erp.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * ERP服务Kafka主题配置
 * 
 * @author MK
 */
@Slf4j
@Configuration
public class KafkaTopicConfig implements CommandLineRunner {

    /**
     * BPM流程状态变更主题（与BPM服务保持一致）
     */
    public static final String BPM_PROCESS_STATUS_TOPIC = "bpm.process.status.changed";

    @Override
    public void run(String... args) throws Exception {
        log.info("ERP服务Kafka配置已准备，监听主题：{}", BPM_PROCESS_STATUS_TOPIC);
        log.info("如果主题不存在，Kafka将自动创建（需要开启auto.create.topics.enable=true）");
    }
} 