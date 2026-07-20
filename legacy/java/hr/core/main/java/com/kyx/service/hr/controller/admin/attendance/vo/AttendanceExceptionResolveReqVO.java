package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 考勤异常处理 Request VO")
@Data
public class AttendanceExceptionResolveReqVO {

    @Schema(description = "异常ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "异常ID不能为空")
    private Long id;

    @Schema(description = "处理状态")
    private String exceptionStatus;

    @Schema(description = "处理备注")
    private String handleRemark;

}
