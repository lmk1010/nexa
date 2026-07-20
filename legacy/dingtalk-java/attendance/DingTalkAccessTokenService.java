package com.kyx.service.hr.service.attendance.dingtalk;

/**
 * DingTalk access token service.
 *
 * The token is cached in memory and refreshed before expiration.
 */
public interface DingTalkAccessTokenService {

    /**
     * Returns a valid access token.
     *
     * @return DingTalk access token
     */
    String getAccessToken();

    /**
     * Clears the current cached token.
     */
    void evict();

}

