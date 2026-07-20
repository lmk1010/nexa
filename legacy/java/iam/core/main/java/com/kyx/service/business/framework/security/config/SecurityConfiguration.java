package com.kyx.service.business.framework.security.config;

import com.kyx.foundation.security.config.AuthorizeRequestsCustomizer;
import com.kyx.service.business.enums.ApiConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * System 模块的 Security 配置
 */
@Configuration(proxyBeanMethods = false, value = "systemSecurityConfiguration")
public class SecurityConfiguration {

    @Bean("systemAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer authorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {

            @Override
            public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
                // TODO ：这个每个项目都需要重复配置，得捉摸有没通用的方案
                // Swagger 接口文档
                registry.requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/swagger-ui").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll();
                // Druid 监控
                registry.requestMatchers("/druid/**").permitAll();
                // Spring Boot Actuator 的安全配置
                registry.requestMatchers("/actuator").permitAll()
                        .requestMatchers("/actuator/**").permitAll();
                // RPC 服务的安全配置
                registry.requestMatchers(ApiConstants.PREFIX + "/**").permitAll();
                
                // App API 认证接口白名单 - 移动端登录相关接口
                registry.requestMatchers(buildAppApi("/app/auth/login")).permitAll()
                        .requestMatchers(buildAppApi("/app/auth/register")).permitAll()
                                                .requestMatchers(buildAppApi("/app/auth/send-sms-code")).permitAll()
                        .requestMatchers(buildAppApi("/app/auth/reset-password")).permitAll();
                
                // 兼容Flutter应用的路径 - 实际路径是/app/auth，但Flutter调用的是/app-api/system/app/auth
                registry.requestMatchers("/app-api/system/app/auth/login").permitAll()
                        .requestMatchers("/app-api/system/app/auth/register").permitAll()
                        .requestMatchers("/app-api/system/app/auth/send-sms-code").permitAll()
                        .requestMatchers("/app-api/system/app/auth/reset-password").permitAll();
            }

        };
    }

}
