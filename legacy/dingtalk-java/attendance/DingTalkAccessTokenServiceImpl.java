package com.kyx.service.hr.service.attendance.dingtalk;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.kyx.service.hr.config.DingTalkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DingTalk access token service implementation.
 */
@Service
public class DingTalkAccessTokenServiceImpl implements DingTalkAccessTokenService {

    private static final Logger log = LoggerFactory.getLogger(DingTalkAccessTokenServiceImpl.class);
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;

    private final RestTemplate restTemplate = buildDirectRestTemplate();
    @Resource
    private DingTalkProperties dingTalkProperties;

    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile CachedToken cachedToken;

    @Override
    public String getAccessToken() {
        CachedToken token = cachedToken;
        if (isUsable(token)) {
            return token.getAccessToken();
        }

        refreshLock.lock();
        try {
            token = cachedToken;
            if (isUsable(token)) {
                return token.getAccessToken();
            }
            CachedToken newToken = fetchAccessToken();
            cachedToken = newToken;
            return newToken.getAccessToken();
        } finally {
            refreshLock.unlock();
        }
    }

    @Override
    public void evict() {
        cachedToken = null;
    }

    private boolean isUsable(CachedToken token) {
        return token != null && Instant.now().getEpochSecond() < token.getRefreshAtEpochSeconds();
    }

    private CachedToken fetchAccessToken() {
        String appKey = trim(dingTalkProperties.getApp().getAppKey());
        String appSecret = trim(dingTalkProperties.getApp().getAppSecret());
        String endpoint = trim(dingTalkProperties.getAccessToken().getEndpoint());

        if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret)) {
            throw new IllegalStateException("Missing dingtalk.app.app-key or dingtalk.app.app-secret");
        }
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException("Missing dingtalk.access-token.endpoint");
        }

        AccessTokenRequest requestBody = new AccessTokenRequest();
        requestBody.setAppKey(appKey);
        requestBody.setAppSecret(appSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AccessTokenRequest> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<AccessTokenResponse> response;
        try {
            response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, AccessTokenResponse.class);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.error("Fetch DingTalk access token failed, status={}, body={}", ex.getRawStatusCode(), body);
            throw new IllegalStateException("Fetch DingTalk access token failed: " + body, ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Fetch DingTalk access token failed: " + ex.getMessage(), ex);
        }

        AccessTokenResponse body = response.getBody();
        if (body == null || !StringUtils.hasText(body.getAccessToken()) || body.getExpireIn() == null) {
            throw new IllegalStateException("Invalid DingTalk access token response");
        }

        long now = Instant.now().getEpochSecond();
        long expireIn = Math.max(body.getExpireIn(), 1L);
        long refreshBeforeSeconds = Math.max(safeLong(dingTalkProperties.getAccessToken().getRefreshBeforeSeconds(), 300L), 0L);
        long refreshAt = now + Math.max(expireIn - refreshBeforeSeconds, 1L);

        log.info("Fetched DingTalk access token, expireIn={}s, refreshBefore={}s", expireIn, refreshBeforeSeconds);
        return new CachedToken(body.getAccessToken(), refreshAt);
    }

    private long safeLong(Long value, long defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private RestTemplate buildDirectRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    private static class AccessTokenRequest {

        private String appKey;

        private String appSecret;

        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

    }

    private static class AccessTokenResponse {

        @JsonAlias({"accessToken", "access_token"})
        private String accessToken;

        @JsonAlias({"expireIn", "expiresIn", "expires_in"})
        private Long expireIn;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public Long getExpireIn() {
            return expireIn;
        }

        public void setExpireIn(Long expireIn) {
            this.expireIn = expireIn;
        }

    }

    private static class CachedToken {

        private final String accessToken;

        private final long refreshAtEpochSeconds;

        private CachedToken(String accessToken, long refreshAtEpochSeconds) {
            this.accessToken = accessToken;
            this.refreshAtEpochSeconds = refreshAtEpochSeconds;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public long getRefreshAtEpochSeconds() {
            return refreshAtEpochSeconds;
        }

    }

}
