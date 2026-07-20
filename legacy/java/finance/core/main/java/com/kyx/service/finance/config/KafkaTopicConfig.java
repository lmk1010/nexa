package com.kyx.service.finance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka主题配置
 * 
 * @author xyang
 */
@Slf4j
@Configuration
public class KafkaTopicConfig implements CommandLineRunner {

    /**
     * BPM流程状态变更主题
     */
    public static final String FINANCE_PROCESS_STATUS_TOPIC = "finance.process.status.changed";

    @Override
    public void run(String... args) throws Exception {
        log.info("Kafka主题配置已准备：{}", FINANCE_PROCESS_STATUS_TOPIC);
        log.info("如果主题不存在，Kafka将自动创建（需要开启auto.create.topics.enable=true）");
        log.info("或者手动创建主题：kafka-topics.sh --create --topic {} --partitions 3 --replication-factor 1", FINANCE_PROCESS_STATUS_TOPIC);
    }
} 