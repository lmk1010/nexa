package com.kyx.service.finance.service.support;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 财务关键写操作防护：
 * 1) 数据库锁冲突自动重试（死锁/锁等待）
 * 2) 接口级幂等（Redis setIfAbsent）
 *
 * @author xyang
 */
@Service
@Slf4j
public class FinanceExecutionGuardService {

    private static final String IDEMPOTENT_KEY_PREFIX = "finance:idempotent:";
    private static final int DB_RETRY_MAX_TIMES = 3;
    private static final long DB_RETRY_SLEEP_MILLIS = 120L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public <T> T executeWithIdempotentAndRetry(String operation, String bizKey, long timeoutSeconds, Supplier<T> supplier) {
        String lockKey = buildIdempotentKey(operation, bizKey);
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "", timeoutSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(success)) {
            throw exception(GlobalErrorCodeConstants.REPEATED_REQUESTS);
        }
        try {
            return executeWithDbRetry(operation, supplier);
        } catch (RuntimeException ex) {
            // 发生异常后释放幂等键，避免失败请求长期占用
            stringRedisTemplate.delete(lockKey);
            throw ex;
        }
    }

    public <T> T executeWithDbRetry(String operation, Supplier<T> supplier) {
        for (int attempt = 1; attempt <= DB_RETRY_MAX_TIMES; attempt++) {
            try {
                return supplier.get();
            } catch (RuntimeException ex) {
                if (!isRetryableLockException(ex)) {
                    throw ex;
                }
                if (attempt >= DB_RETRY_MAX_TIMES) {
                    log.warn("[executeWithDbRetry][operation({}) 重试耗尽，返回并发冲突]", operation, ex);
                    throw exception(GlobalErrorCodeConstants.LOCKED);
                }
                log.warn("[executeWithDbRetry][operation({}) 第{}次遇到锁冲突，准备重试]", operation, attempt, ex);
                sleepQuietly(DB_RETRY_SLEEP_MILLIS * attempt);
            }
        }
        throw exception(GlobalErrorCodeConstants.LOCKED);
    }

    private String buildIdempotentKey(String operation, String bizKey) {
        String normalizedOperation = StringUtils.hasText(operation) ? operation.trim() : "unknown";
        String normalizedBizKey = StringUtils.hasText(bizKey) ? bizKey.trim() : "empty";
        Long userId = getLoginUserId();
        String userPart = userId == null ? "anonymous" : String.valueOf(userId);
        String digest = DigestUtils.md5DigestAsHex(normalizedBizKey.getBytes(StandardCharsets.UTF_8));
        return IDEMPOTENT_KEY_PREFIX + normalizedOperation + ":" + userPart + ":" + digest;
    }

    private boolean isRetryableLockException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DeadlockLoserDataAccessException
                    || current instanceof CannotAcquireLockException
                    || current instanceof PessimisticLockingFailureException
                    || current instanceof CannotSerializeTransactionException) {
                return true;
            }
            String message = current.getMessage();
            if (StringUtils.hasText(message)) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("deadlock")
                        || normalized.contains("lock wait timeout")
                        || normalized.contains("could not obtain lock")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw exception(GlobalErrorCodeConstants.LOCKED);
        }
    }
}
