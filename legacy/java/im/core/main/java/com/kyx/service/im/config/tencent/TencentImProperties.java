package com.kyx.service.im.config.tencent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "kyx.tencent-im")
public class TencentImProperties {

    private long sdkAppId;

    private String secretKey;

    private long expireSeconds = 604800L;

    private String fixedPrefix = "";

    private String defaultOrdersysUserPrefix = "EMPLOYEE";

    private boolean autoCreateMapping = false;
}
