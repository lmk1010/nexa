package com.kyx.service.im.api.invitecode.dto;

import lombok.Data;

/**
 * 邀请码验证 Response DTO
 *
 * @author MK
 */
@Data
public class InviteCodeValidateRespDTO {

    /**
     * 是否有效
     */
    private Boolean valid;

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 租户状态
     */
    private Integer tenantStatus;

    /**
     * 邀请码
     */
    private String code;

    /**
     * 邀请码类型
     */
    private Integer type;

    /**
     * 使用次数限制
     */
    private Integer usageLimit;

    /**
     * 已使用次数
     */
    private Integer usedCount;

    /**
     * 状态
     */
    private Integer status;
} 