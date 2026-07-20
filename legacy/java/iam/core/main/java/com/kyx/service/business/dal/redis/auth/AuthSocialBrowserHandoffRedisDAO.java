package com.kyx.service.business.dal.redis.auth;

import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.service.business.dal.dataobject.auth.AuthSocialBrowserHandoffCacheDO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.kyx.service.business.dal.redis.RedisKeyConstants.AUTH_SOCIAL_BROWSER_HANDOFF;

@Repository
public class AuthSocialBrowserHandoffRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public AuthSocialBrowserHandoffCacheDO get(String handoffId) {
        return JsonUtils.parseObject(
                stringRedisTemplate.opsForValue().get(formatKey(handoffId)),
                AuthSocialBrowserHandoffCacheDO.class
        );
    }

    public void set(String handoffId, AuthSocialBrowserHandoffCacheDO cacheDO, long timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                formatKey(handoffId),
                JsonUtils.toJsonString(cacheDO),
                timeoutSeconds,
                TimeUnit.SECONDS
        );
    }

    public void delete(String handoffId) {
        stringRedisTemplate.delete(formatKey(handoffId));
    }

    private static String formatKey(String handoffId) {
        return String.format(AUTH_SOCIAL_BROWSER_HANDOFF, handoffId);
    }

}
