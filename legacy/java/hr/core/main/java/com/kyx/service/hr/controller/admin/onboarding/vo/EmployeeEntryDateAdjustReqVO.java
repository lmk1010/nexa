package com.kyx.service.hr.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 员工入职日期调整 Request VO")
@Data
public class EmployeeEntryDateAdjustReqVO {

    @Schema(description = "入职记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入职记录ID不能为空")
    private Long id;

    @Schema(description = "新的入职日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "入职日期不能为空")
    private String newEntryDate;

} 