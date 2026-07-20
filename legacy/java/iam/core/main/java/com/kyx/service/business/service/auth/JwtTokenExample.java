package com.kyx.service.business.service.auth;

import com.kyx.service.business.config.JwtTokenConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * JWT Token 使用示例
 * 
 * 展示如何使用新的JWT token系统，以及与auth.go的兼容性
 * 
 * @author MK
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
@Slf4j
public class JwtTokenExample {

    @Resource
    private JwtTokenUtil jwtTokenUtil;
    
    @Resource
    private JwtTokenConfig jwtTokenConfig;

    /**
     * 系统启动后输出JWT配置信息
     */
    @PostConstruct
    public void printJwtConfig() {
        log.info("=== JWT Token 配置信息 ===");
        log.info("JWT 启用状态: {}", jwtTokenConfig.getEnabled());
        log.info("JWT 过期时间: {} 秒", jwtTokenConfig.getExpireSeconds());
        log.info("JWT 签发者: {}", jwtTokenConfig.getIssuer());
        log.info("JWT 密钥长度: {} 字符", jwtTokenConfig.getSecret().length());
        
        if (jwtTokenConfig.getEnabled()) {
            // 生成示例token
            String userId = "1001";
            Integer platformId = 1; // 1表示管理端，对应auth.go中的constant.AdminPlatformID
            LocalDateTime expiresTime = LocalDateTime.now().plusSeconds(jwtTokenConfig.getExpireSeconds());
            
            String token = jwtTokenUtil.generateToken(userId, platformId, expiresTime);
            log.info("示例JWT Token: {}", token);
            
            // 验证token
            JwtTokenUtil.Claims claims = jwtTokenUtil.parseToken(token);
            if (claims != null) {
                log.info("Token验证成功 - 用户ID: {}, 平台ID: {}, 过期时间: {}", 
                    claims.userID, claims.platformID, claims.exp);
            } else {
                log.warn("Token验证失败");
            }
        }
        
        log.info("========================");
    }

    /**
     * 示例：生成移动端用户token
     */
    public String generateMobileUserToken(String userId) {
        Integer platformId = 2; // 2表示移动端
        LocalDateTime expiresTime = LocalDateTime.now().plusSeconds(jwtTokenConfig.getExpireSeconds());
        return jwtTokenUtil.generateToken(userId, platformId, expiresTime);
    }

    /**
     * 示例：生成管理端用户token  
     */
    public String generateAdminUserToken(String userId) {
        Integer platformId = 1; // 1表示管理端，对应auth.go中的constant.AdminPlatformID
        LocalDateTime expiresTime = LocalDateTime.now().plusSeconds(jwtTokenConfig.getExpireSeconds());
        return jwtTokenUtil.generateToken(userId, platformId, expiresTime);
    }

    /**
     * 示例：验证token并获取用户信息
     */
    public JwtTokenUtil.Claims validateToken(String token) {
        return jwtTokenUtil.parseToken(token);
    }
} 