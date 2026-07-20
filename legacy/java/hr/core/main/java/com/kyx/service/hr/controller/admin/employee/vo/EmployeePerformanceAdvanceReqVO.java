package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 绩效周期快速推进 Request VO")
@Data
public class EmployeePerformanceAdvanceReqVO {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "绩效记录ID不能为空")
    private Long id;
}
