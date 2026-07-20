package com.kyx.service.hr.integration.dingtalk.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.service.hr.config.DingTalkProperties;
import com.kyx.service.hr.service.attendance.dingtalk.DingTalkAccessTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight DingTalk OpenAPI client.
 */
@Component
@Slf4j
public class DingTalkOpenApiClient {

    private static final String OAPI_HOST = "https://oapi.dingtalk.com";
    private static final String OPEN_API_HOST = "https://api.dingtalk.com";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int MAX_API_ATTEMPTS = 5;
    private static final long RATE_LIMIT_BASE_BACKOFF_MS = 1500L;
    private static final long RATE_LIMIT_MAX_BACKOFF_MS = 15000L;

    private static final AtomicLong NEXT_ALLOWED_CALL_EPOCH_MS = new AtomicLong(0L);

    private final RestTemplate restTemplate = buildDirectRestTemplate();
    @Resource
    private DingTalkAccessTokenService accessTokenService;
    @Resource
    private DingTalkProperties dingTalkProperties;

    public JsonNode postTopApi(String apiPath, Object requestBody) {
        String token = accessTokenService.getAccessToken();
        return doPost(apiPath, token, requestBody);
    }

    public JsonNode postOpenApi(String apiPath, Object requestBody) {
        String token = accessTokenService.getAccessToken();
        return doOpenApiPost(apiPath, token, requestBody);
    }

    public JsonNode postTopApiWithRetry(String apiPath, Object requestBody) {
        return postWithRetry(apiPath, () -> postTopApi(apiPath, requestBody));
    }

    public JsonNode postOpenApiWithRetry(String apiPath, Object requestBody) {
        return postWithRetry(apiPath, () -> postOpenApi(apiPath, requestBody));
    }

    private JsonNode postWithRetry(String apiPath, ApiCall call) {
        RuntimeException lastException = null;
        boolean tokenEvicted = false;
        for (int attempt = 0; attempt < MAX_API_ATTEMPTS; attempt++) {
            try {
                return call.execute();
            } catch (RuntimeException ex) {
                lastException = ex;
                boolean hasMoreAttempts = attempt + 1 < MAX_API_ATTEMPTS;
                if (hasMoreAttempts && isRateLimitException(ex)) {
                    long waitMs = resolveRateLimitBackoffMs(attempt);
                    log.warn("DingTalk API rate limited, retrying, path={}, attempt={}, waitMs={}, reason={}",
                            apiPath, attempt + 1, waitMs, resolveExceptionMessage(ex));
                    sleepQuietly(waitMs);
                    continue;
                }
                if (hasMoreAttempts && !tokenEvicted) {
                    tokenEvicted = true;
                    accessTokenService.evict();
                    continue;
                }
                throw ex;
            }
        }
        throw lastException == null ? new IllegalStateException("DingTalk API call failed: " + apiPath) : lastException;
    }

    private JsonNode doPost(String apiPath, String accessToken, Object requestBody) {
        acquireGlobalQpsPermit();

        String tokenParam;
        try {
            tokenParam = URLEncoder.encode(accessToken, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("URL encode token failed", e);
        }
        String url = OAPI_HOST + apiPath + "?access_token=" + tokenParam;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        HttpEntity<String> entity = new HttpEntity<>(JsonUtils.toJsonString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        String text = response.getBody();
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException("Empty response from DingTalk API: " + apiPath);
        }
        JsonNode root = JsonUtils.parseTree(text);
        Integer errcode = readInt(root, "errcode");
        if (errcode != null && errcode != 0) {
            String errmsg = readText(root, "errmsg");
            log.warn("DingTalk API failed, path={}, errcode={}, errmsg={}", apiPath, errcode, errmsg);
            throw new DingTalkApiException(apiPath, errcode, errmsg);
        }
        return root;
    }

    private JsonNode doOpenApiPost(String apiPath, String accessToken, Object requestBody) {
        acquireGlobalQpsPermit();
        String url = OPEN_API_HOST + apiPath;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        headers.set("x-acs-dingtalk-access-token", accessToken);
        HttpEntity<String> entity = new HttpEntity<>(JsonUtils.toJsonString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        String text = response.getBody();
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException("Empty response from DingTalk API: " + apiPath);
        }
        JsonNode root = JsonUtils.parseTree(text);
        assertSuccessfulResponse(apiPath, root);
        return root;
    }

    private void assertSuccessfulResponse(String apiPath, JsonNode root) {
        Integer errcode = readInt(root, "errcode");
        if (errcode != null && errcode != 0) {
            String errmsg = readText(root, "errmsg");
            log.warn("DingTalk API failed, path={}, errcode={}, errmsg={}", apiPath, errcode, errmsg);
            throw new DingTalkApiException(apiPath, errcode, errmsg);
        }

        Boolean success = readBoolean(root, "success");
        String code = readText(root, "code");
        String message = readText(root, "message");
        if (Boolean.FALSE.equals(success)) {
            String errmsg = StringUtils.hasText(message) ? message : code;
            log.warn("DingTalk API failed, path={}, code={}, message={}", apiPath, code, message);
            throw new DingTalkApiException(apiPath, -1, errmsg);
        }
        if (StringUtils.hasText(code) && !"0".equals(code) && !"OK".equalsIgnoreCase(code)
                && !root.has("result")) {
            String errmsg = StringUtils.hasText(message) ? code + ":" + message : code;
            log.warn("DingTalk API failed, path={}, code={}, message={}", apiPath, code, message);
            throw new DingTalkApiException(apiPath, -1, errmsg);
        }
    }

    private void acquireGlobalQpsPermit() {
        long minIntervalMs = resolveMinIntervalMs();
        if (minIntervalMs <= 0) {
            return;
        }
        while (true) {
            long now = System.currentTimeMillis();
            long nextAllowed = NEXT_ALLOWED_CALL_EPOCH_MS.get();
            long slot = Math.max(now, nextAllowed);
            long newNext = slot + minIntervalMs;
            if (!NEXT_ALLOWED_CALL_EPOCH_MS.compareAndSet(nextAllowed, newNext)) {
                continue;
            }
            long waitMs = slot - now;
            if (waitMs <= 0) {
                return;
            }
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return;
        }
    }

    private long resolveMinIntervalMs() {
        Integer maxQps = dingTalkProperties.getApi() == null ? null : dingTalkProperties.getApi().getMaxQps();
        if (maxQps == null || maxQps <= 0) {
            return 0L;
        }
        return Math.max(1000L / maxQps, 1L);
    }

    private boolean isRateLimitException(RuntimeException ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof DingTalkApiException) {
                DingTalkApiException apiException = (DingTalkApiException) cursor;
                if (apiException.getErrcode() == 88 || apiException.getErrcode() == 90002) {
                    return true;
                }
            }
            String message = cursor.getMessage();
            if (StringUtils.hasText(message)) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("90002")
                        || normalized.contains("qps")
                        || normalized.contains("rate limit")
                        || normalized.contains("too many")) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private long resolveRateLimitBackoffMs(int attempt) {
        int exponent = Math.min(Math.max(attempt, 0), 3);
        long backoff = RATE_LIMIT_BASE_BACKOFF_MS << exponent;
        return Math.min(backoff, RATE_LIMIT_MAX_BACKOFF_MS);
    }

    private void sleepQuietly(long waitMs) {
        if (waitMs <= 0) {
            return;
        }
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String resolveExceptionMessage(Throwable ex) {
        Throwable cursor = ex;
        while (cursor != null && cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor == null ? null : cursor.getMessage();
        return StringUtils.hasText(message) ? message : ex.getClass().getSimpleName();
    }

    public Map<String, Object> body() {
        return new HashMap<>();
    }

    private Integer readInt(JsonNode node, String name) {
        JsonNode target = node.path(name);
        if (target.isMissingNode() || target.isNull()) {
            return null;
        }
        if (target.isInt() || target.isLong()) {
            return target.intValue();
        }
        if (target.isTextual()) {
            try {
                return Integer.parseInt(target.textValue());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String readText(JsonNode node, String name) {
        JsonNode target = node.path(name);
        return target.isMissingNode() || target.isNull() ? "" : target.asText("");
    }

    private Boolean readBoolean(JsonNode node, String name) {
        JsonNode target = node.path(name);
        if (target.isMissingNode() || target.isNull()) {
            return null;
        }
        if (target.isBoolean()) {
            return target.asBoolean();
        }
        if (target.isNumber()) {
            return target.asInt() != 0;
        }
        String text = target.asText(null);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "1".equals(normalized) || "success".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "fail".equals(normalized)) {
            return false;
        }
        return null;
    }

    private interface ApiCall {
        JsonNode execute();
    }

    private RestTemplate buildDirectRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    public static final class DingTalkApiException extends IllegalStateException {

        private final String apiPath;
        private final int errcode;
        private final String errmsg;

        private DingTalkApiException(String apiPath, int errcode, String errmsg) {
            super("DingTalk API error: " + errcode + ", " + errmsg);
            this.apiPath = apiPath;
            this.errcode = errcode;
            this.errmsg = errmsg;
        }

        public String getApiPath() {
            return apiPath;
        }

        public int getErrcode() {
            return errcode;
        }

        public String getErrmsg() {
            return errmsg;
        }
    }

}
