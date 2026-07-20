package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "Admin - Attendance overtime approve Request VO")
@Data
public class AttendanceOvertimeApproveReqVO {

    @Schema(description = "Application id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "申请ID不能为空")
    private Long id;

    @Schema(description = "Approve result", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @Schema(description = "Approve remark")
    private String approveRemark;

}
