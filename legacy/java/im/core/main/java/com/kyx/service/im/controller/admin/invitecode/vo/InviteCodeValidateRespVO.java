package com.kyx.service.im.controller.admin.invitecode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 邀请码验证 Response VO")
@Data
public class InviteCodeValidateRespVO {

    @Schema(description = "是否有效", example = "true")
    private Boolean valid;

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "租户名称", example = "示例公司")
    private String tenantName;

    @Schema(description = "租户状态", example = "1")
    private Integer tenantStatus;

    @Schema(description = "邀请码", example = "INVITE123")
    private String code;

    @Schema(description = "邀请码类型", example = "1")
    private Integer type;

    @Schema(description = "使用次数限制", example = "100")
    private Integer usageLimit;

    @Schema(description = "已使用次数", example = "10")
    private Integer usedCount;

    @Schema(description = "状态", example = "0")
    private Integer status;
} 