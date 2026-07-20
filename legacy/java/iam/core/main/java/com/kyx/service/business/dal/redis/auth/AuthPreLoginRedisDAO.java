package com.kyx.service.business.dal.redis.auth;

import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.service.business.dal.dataobject.auth.AuthPreLoginCacheDO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.kyx.service.business.dal.redis.RedisKeyConstants.AUTH_PRE_LOGIN_TOKEN;

@Repository
public class AuthPreLoginRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public AuthPreLoginCacheDO get(String preAuthToken) {
        return JsonUtils.parseObject(
                stringRedisTemplate.opsForValue().get(formatKey(preAuthToken)),
                AuthPreLoginCacheDO.class
        );
    }

    public void set(String preAuthToken, AuthPreLoginCacheDO cacheDO, long timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                formatKey(preAuthToken),
                JsonUtils.toJsonString(cacheDO),
                timeoutSeconds,
                TimeUnit.SECONDS
        );
    }

    public void delete(String preAuthToken) {
        stringRedisTemplate.delete(formatKey(preAuthToken));
    }

    private static String formatKey(String preAuthToken) {
        return String.format(AUTH_PRE_LOGIN_TOKEN, preAuthToken);
    }

}
