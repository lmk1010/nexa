package com.kyx.service.hr.dal.dataobject.integration;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * DingTalk user -> OA user binding.
 */
@TableName("hr_dingtalk_user_binding")
@KeySequence("hr_dingtalk_user_binding_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class DingTalkUserBindingDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * DingTalk user id.
     */
    private String dingUserId;

    /**
     * OA user id.
     */
    private Long oaUserId;

    /**
     * HR profile id.
     */
    private Long profileId;

    /**
     * DingTalk name.
     */
    private String dingUserName;

    /**
     * DingTalk mobile.
     */
    private String dingMobile;

    /**
     * DingTalk email.
     */
    private String dingEmail;

    /**
     * DingTalk primary dept id.
     */
    private Long dingDeptId;

    /**
     * DingTalk active status.
     */
    private Boolean dingActive;

    /**
     * Mapping strategy: MOBILE / NAME.
     */
    private String matchType;

    /**
     * Data source: AUTO_SYNC / MANUAL.
     */
    private String sourceType;

    /**
     * Last event time seen from DingTalk side.
     */
    private LocalDateTime lastSeenTime;

    /**
     * Last sync time.
     */
    private LocalDateTime syncTime;

    /**
     * Raw payload snapshot.
     */
    private String rawPayload;

}
