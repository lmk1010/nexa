package com.kyx.service.im.dal.mysql.invitecode;

import lombok.Data;

/**
 * 租户邀请码统计 VO
 *
 * @author MK
 */
@Data
public class TenantInviteCodeStatsVO {

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 邀请码总数
     */
    private Long totalCount;

    /**
     * 启用状态邀请码数量
     */
    private Long enabledCount;

    /**
     * 禁用状态邀请码数量
     */
    private Long disabledCount;

    /**
     * 已过期邀请码数量
     */
    private Long expiredCount;

    /**
     * 已用完邀请码数量
     */
    private Long usedUpCount;

    /**
     * 注册邀请码数量
     */
    private Long registerCount;

    /**
     * 群组邀请码数量
     */
    private Long groupCount;

    /**
     * 频道邀请码数量
     */
    private Long channelCount;

} 