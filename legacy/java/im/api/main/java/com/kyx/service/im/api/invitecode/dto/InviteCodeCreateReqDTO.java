package com.kyx.service.im.api.invitecode.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 邀请码创建 Request DTO
 *
 * @author MK
 */
@Data
public class InviteCodeCreateReqDTO {

    /**
     * 邀请码
     */
    private String code;

    /**
     * 邀请码类型
     */
    @NotNull(message = "邀请码类型不能为空")
    private Integer type;

    /**
     * 使用次数限制
     */
    private Integer usageLimit;

    /**
     * 有效期开始时间
     */
    private LocalDateTime validStartTime;

    /**
     * 有效期结束时间
     */
    private LocalDateTime validEndTime;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

} 