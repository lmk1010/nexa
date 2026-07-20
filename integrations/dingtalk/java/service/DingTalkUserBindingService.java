package com.kyx.service.hr.integration.dingtalk.service;

import java.util.Map;

/**
 * Resolves user binding between DingTalk and OA.
 *
 * Keep this contract stable so we can switch from mobile matching
 * to explicit binding table without changing sync workflows.
 */
public interface DingTalkUserBindingService {

    /**
     * Build mapping: DingTalk userId -> OA userId.
     */
    Map<String, Long> buildDingUserToOaUserMap();

    /**
     * Return locally cached mapping only (no remote API).
     */
    default Map<String, Long> getCachedDingUserToOaUserMap() {
        return buildDingUserToOaUserMap();
    }

    /**
     * Refresh mapping from upstream source.
     */
    default Map<String, Long> refreshDingUserToOaUserMap() {
        return buildDingUserToOaUserMap();
    }

    /**
     * Refresh in-memory cache from local binding table only.
     */
    default Map<String, Long> refreshCacheFromLocalBindings() {
        return getCachedDingUserToOaUserMap();
    }

    /**
     * Resolve DingTalk userId by OA userId.
     */
    String findDingUserIdByOaUserId(Long oaUserId);

}
