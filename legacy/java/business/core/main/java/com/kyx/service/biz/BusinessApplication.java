package com.kyx.service.biz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * KYX 模块化单体启动类。
 *
 * <p>IAM 和 Gateway 保持独立；OP、BPM、HR、ERP、Finance、IM、AI 与 Business
 * 共享当前 Spring 容器。领域源码继续保留在各自 Maven 模块中，避免把模块边界
 * 退化成一个无法维护的大包。</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {
                "com.kyx.service.biz",
                "com.kyx.service.op",
                "com.kyx.service.bpm",
                "com.kyx.service.hr",
                "com.kyx.service.erp",
                "com.kyx.service.finance",
                "com.kyx.service.im",
                "com.kyx.service.ai"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = {
                // 排除各模块原有启动类，避免它们再次触发各自的全包扫描。
                // 用 REGEX，避免 ASSIGNABLE_TYPE 在解析启动注解时强制加载各领域 Application 类。
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = SpringBootApplication.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern =
                        "com\\.kyx\\.service\\.(op\\.InfraServerApplication"
                                + "|bpm\\.BpmServerApplication"
                                + "|hr\\.HRApplication"
                                + "|erp\\.ErpServerApplication"
                                + "|finance\\.FinanceServerApplication"
                                + "|im\\.IMManagerApplication"
                                + "|ai\\.AIApplication)"),
                // 单体内只保留 MonolithRpcConfiguration；领域间调用直接注入本地实现。
                @ComponentScan.Filter(type = FilterType.REGEX,
                        pattern = "com\\.kyx\\.service\\.(biz|op|bpm|hr|erp|finance|im|ai)"
                                + "\\.framework\\.rpc\\.config\\.RpcConfiguration")
        }
)
public class BusinessApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessApplication.class, args);
    }

}
