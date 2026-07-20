package com.kyx.service.im.api.invitecode.dto;

import com.kyx.foundation.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 邀请码分页 Request DTO
 *
 * @author MK
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InviteCodePageReqDTO extends PageParam {

    /**
     * 邀请码
     */
    private String code;

    /**
     * 邀请码类型
     */
    private Integer type;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建人姓名
     */
    private String creatorName;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 创建时间
     */
    private LocalDateTime[] createTime;

} 