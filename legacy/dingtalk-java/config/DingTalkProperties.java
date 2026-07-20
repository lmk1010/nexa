package com.kyx.service.hr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DingTalk integration properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "dingtalk")
public class DingTalkProperties {

    private App app = new App();

    private AccessToken accessToken = new AccessToken();

    private Api api = new Api();

    private Sync sync = new Sync();

    private Notify notify = new Notify();

    private Stream stream = new Stream();

    @Data
    public static class App {

        private String corpId;

        private String appId;

        private String agentId;

        private String appKey;

        private String appSecret;

    }

    @Data
    public static class AccessToken {

        private String endpoint = "https://api.dingtalk.com/v1.0/oauth2/accessToken";

        /**
         * Refresh token this many seconds before hard expiration.
         */
        private Long refreshBeforeSeconds = 300L;

    }

    @Data
    public static class Api {

        /**
         * Global DingTalk OpenAPI max requests per second.
         */
        private Integer maxQps = 2;

    }

    @Data
    public static class Sync {

        private Boolean enabled = false;

        private Long fixedDelayMs = 86_400_000L;

        /**
         * Delay employee sync after scheduler trigger to avoid DingTalk top-of-hour throttling.
         */
        private Long employeeScheduleStartDelayMs = 0L;

        private Long attendanceLookbackMinutes = 1_440L;

        private Integer rootDeptId = 1;

        private Boolean employeeEnabled = true;

        /**
         * Sync DingTalk intelligent HR roster fields into OA employee profile/custom fields.
         */
        private Boolean rosterEnabled = true;

        /**
         * Allow DingTalk roster fields to overwrite OA employee profile fields.
         * Keep disabled when OA is the workflow/source-of-truth owner.
         */
        private Boolean rosterOverwriteProfileEnabled = false;

        /**
         * Backfill OA employee profile fields only when they are empty.
         */
        private Boolean rosterBackfillProfileEmptyOnly = true;

        /**
         * Auto create OA user + HR profile when DingTalk user is not matched.
         */
        private Boolean employeeAutoCreateEnabled = true;

        /**
         * Target OA dept for auto-created employees.
         */
        private Long employeeAutoCreateDeptId = 1L;

        /**
         * Optional fallback OA dept name when DingTalk dept cannot be matched.
         * Blank means using the first valid configured department instead of a hardcoded company name.
         */
        private String employeeFallbackDeptName;

        private Boolean attendanceEnabled = true;

        private Boolean leaveEnabled = true;

        /**
         * Leave sync lookback days for scheduled/manual compensation.
         */
        private Integer leaveLookbackDays = 1;

        /**
         * Leave sync forward days for scheduled/manual compensation.
         * Future approved leaves are mainly handled by DingTalk stream events.
         */
        private Integer leaveForwardDays = 0;

        /**
         * Enrich each user with /topapi/v2/user/get to avoid missing fields from /topapi/v2/user/list.
         */
        private Boolean userDetailEnabled = true;

        /**
         * Sleep interval between user detail calls to avoid trigger DingTalk rate limit.
         */
        private Long userDetailIntervalMs = 120L;

        /**
         * Sleep interval between attendance listRecord batches.
         */
        private Long attendanceBatchIntervalMs = 300L;

        /**
         * Sleep interval between DingTalk leave duration calls.
         */
        private Long leaveBatchIntervalMs = 120L;

        /**
         * Sleep interval between intelligent HR roster batches.
         */
        private Long rosterBatchIntervalMs = 200L;

    }

    @Data
    public static class Notify {

        private Boolean enabled = false;

        /**
         * text message by default.
         */
        private String messageType = "text";

    }

    @Data
    public static class Stream {

        /**
         * Enable DingTalk Stream event consumption.
         */
        private Boolean enabled = false;

        /**
         * SDK event consume thread count.
         */
        private Integer consumeThreads = 4;

        /**
         * Coalesce attendance-triggered sync calls to avoid flood.
         */
        private Long attendanceSyncMinIntervalSeconds = 30L;

        /**
         * Time range lookback for each stream-triggered attendance sync.
         */
        private Long attendanceSyncLookbackMinutes = 15L;

        /**
         * Coalesce directory-triggered employee sync calls to avoid floods.
         */
        private Long employeeSyncMinIntervalSeconds = 60L;

        /**
         * Leave sync date window before event day.
         */
        private Integer leaveSyncLookbackDays = 3;

        /**
         * Leave sync date window after event day.
         */
        private Integer leaveSyncForwardDays = 31;

    }

}
