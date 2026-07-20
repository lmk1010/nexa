package com.kyx.service.business.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT Token 配置类
 * 
 * 用于配置JWT token的生成和验证参数，与auth.go中的JWT配置保持一致
 * 
 * @author MK
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
@ConfigurationProperties(prefix = "kyx.jwt")
@Data
public class JwtTokenConfig {

    /**
     * JWT 密钥
     * 需要与 auth.go 中的 secret 保持一致
     */
    private String secret = "openIM123";

    /**
     * JWT token 过期时间（秒）
     * 默认7天
     */
    private Long expireSeconds = 7 * 24 * 60 * 60L;

    /**
     * JWT 签发者
     */
    private String issuer = "kyx-system";

    /**
     * 是否启用JWT格式token
     * true: 生成JWT格式token
     * false: 使用原来的UUID格式token
     */
    private Boolean enabled = true;

} 