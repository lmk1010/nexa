package com.kyx.service.erp;

import com.kyx.foundation.tenant.core.mq.kafka.TenantKafkaEnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目的启动类
 * <p>
 *
 * @author MK
 */
@SpringBootApplication
public class ErpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ErpServerApplication.class, args);
    }

}
