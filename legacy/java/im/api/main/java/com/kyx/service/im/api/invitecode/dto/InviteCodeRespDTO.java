package com.kyx.service.im.api.invitecode.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 邀请码 Response DTO
 *
 * @author MK
 */
@Data
public class InviteCodeRespDTO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 邀请码
     */
    private String code;

    /**
     * 租户ID
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
     * 有效期开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validStartTime;

    /**
     * 有效期结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validEndTime;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建者用户ID
     */
    private Long creatorId;

    /**
     * 创建者用户名
     */
    private String creatorName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

} 