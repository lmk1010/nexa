package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "Admin - Employee profile change approve Request VO")
@Data
public class EmployeeProfileChangeApproveReqVO {

    @Schema(description = "Request id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "申请ID不能为空")
    private Long id;

    @Schema(description = "Approved flag", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @Schema(description = "Approve remark")
    private String approveRemark;

}
