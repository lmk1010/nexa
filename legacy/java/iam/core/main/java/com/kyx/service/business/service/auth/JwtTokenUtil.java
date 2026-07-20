package com.kyx.service.business.service.auth;

import com.kyx.service.business.config.JwtTokenConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 工具类
 * 
 * 实现与 auth.go 兼容的JWT token生成和验证
 * 使用HMAC-SHA256算法签名，与Go语言版本保持一致
 * 
 * @author MK
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
@Slf4j
public class JwtTokenUtil {

    @Resource
    private JwtTokenConfig jwtTokenConfig;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JWT Claims结构，与auth.go中的claims保持一致
     */
    public static class Claims {
        public String userID;
        public Integer platformID;
        public Long exp; // 过期时间 Unix timestamp
        public Long iat; // 签发时间 Unix timestamp  
        public Long nbf; // 生效时间 Unix timestamp
        
        public Claims() {}
        
        public Claims(String userID, Integer platformID, Long exp, Long iat, Long nbf) {
            this.userID = userID;
            this.platformID = platformID;
            this.exp = exp;
            this.iat = iat;
            this.nbf = nbf;
        }
    }

    /**
     * 生成JWT token
     * 
     * @param userId 用户ID
     * @param platformId 平台ID (对应auth.go中的platformID)
     * @param expiresTime 过期时间
     * @return JWT token字符串
     */
    public String generateToken(String userId, Integer platformId, LocalDateTime expiresTime) {
        if (!jwtTokenConfig.getEnabled()) {
            // 如果未启用JWT，返回UUID格式
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
        
        try {
            // JWT Header
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            
            // JWT Claims - 与auth.go中的Claims结构保持一致
            long now = Instant.now().getEpochSecond();
            long exp = expiresTime.toEpochSecond(ZoneOffset.UTC);
            long nbf = now - 60; // 提前1分钟生效，与auth.go保持一致
            
            Claims claims = new Claims(userId, platformId, exp, now, nbf);
            
            // 转换为Map便于JSON序列化
            Map<String, Object> claimsMap = new HashMap<>();
            claimsMap.put("userID", claims.userID);
            claimsMap.put("platformID", claims.platformID);
            claimsMap.put("exp", claims.exp);
            claimsMap.put("iat", claims.iat);
            claimsMap.put("nbf", claims.nbf);
            
            // 编码Header和Claims
            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsString(header));
            String encodedClaims = base64UrlEncode(objectMapper.writeValueAsString(claimsMap));
            
            // 创建签名数据
            String signingInput = encodedHeader + "." + encodedClaims;
            String signature = generateSignature(signingInput, jwtTokenConfig.getSecret());
            
            // 组装JWT
            return signingInput + "." + signature;
            
        } catch (Exception e) {
            log.error("生成JWT token失败", e);
            // 降级到UUID格式
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }

    /**
     * 验证并解析JWT token
     * 
     * @param token JWT token
     * @return Claims对象，如果验证失败返回null
     */
    public Claims parseToken(String token) {
        if (!jwtTokenConfig.getEnabled() || token == null || !token.contains(".")) {
            return null;
        }
        
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            String encodedHeader = parts[0];
            String encodedClaims = parts[1]; 
            String signature = parts[2];
            
            // 验证签名
            String signingInput = encodedHeader + "." + encodedClaims;
            String expectedSignature = generateSignature(signingInput, jwtTokenConfig.getSecret());
            
            if (!signature.equals(expectedSignature)) {
                log.warn("JWT token签名验证失败");
                return null;
            }
            
            // 解析Claims
            String claimsJson = base64UrlDecode(encodedClaims);
            @SuppressWarnings("unchecked")
            Map<String, Object> claimsMap = objectMapper.readValue(claimsJson, Map.class);
            
            Claims claims = new Claims();
            claims.userID = (String) claimsMap.get("userID");
            claims.platformID = (Integer) claimsMap.get("platformID");
            claims.exp = ((Number) claimsMap.get("exp")).longValue();
            claims.iat = ((Number) claimsMap.get("iat")).longValue();
            claims.nbf = ((Number) claimsMap.get("nbf")).longValue();
            
            // 检查过期时间
            long now = Instant.now().getEpochSecond();
            if (claims.exp < now) {
                log.warn("JWT token已过期: exp={}, now={}", claims.exp, now);
                return null;
            }
            
            // 检查生效时间
            if (claims.nbf > now) {
                log.warn("JWT token尚未生效: nbf={}, now={}", claims.nbf, now);
                return null;
            }
            
            return claims;
            
        } catch (Exception e) {
            log.error("解析JWT token失败", e);
            return null;
        }
    }

    /**
     * 生成HMAC-SHA256签名
     */
    private String generateSignature(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(signature);
    }

    /**
     * Base64 URL编码
     */
    private String base64UrlEncode(String data) {
        return base64UrlEncode(data.getBytes(StandardCharsets.UTF_8));
    }
    
    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * Base64 URL解码
     */
    private String base64UrlDecode(String encoded) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(encoded);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
} 