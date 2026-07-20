package com.kyx.service.hr.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 恢复入职申请 Request VO")
@Data
public class OnboardingRestoreReqVO {

    @Schema(description = "入职记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入职记录ID不能为空")
    private Long id;

    @Schema(description = "恢复备注")
    private String remark;

} 