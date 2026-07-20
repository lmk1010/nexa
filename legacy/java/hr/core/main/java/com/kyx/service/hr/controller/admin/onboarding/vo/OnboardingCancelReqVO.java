package com.kyx.service.hr.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 取消入职申请 Request VO")
@Data
public class OnboardingCancelReqVO {

    @Schema(description = "入职记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入职记录ID不能为空")
    private Long id;

    @Schema(description = "取消原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "取消原因不能为空")
    private String cancelReason;

    @Schema(description = "备注")
    private String remark;
} 