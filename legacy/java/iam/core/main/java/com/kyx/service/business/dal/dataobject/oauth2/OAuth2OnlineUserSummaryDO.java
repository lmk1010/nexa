package com.kyx.service.business.dal.dataobject.oauth2;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户在线状态汇总。
 */
@Data
public class OAuth2OnlineUserSummaryDO {

    /**
     * 用户编号
     */
    private Long userId;

    /**
     * 在线会话数
     */
    private Long onlineSessionCount;

    /**
     * 在线设备数
     */
    private Long onlineDeviceCount;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

}
