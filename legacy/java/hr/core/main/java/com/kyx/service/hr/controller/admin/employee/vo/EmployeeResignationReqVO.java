package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "管理后台 - 员工离职 Request VO")
@Data
public class EmployeeResignationReqVO {

    @Schema(description = "入职记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入职记录ID不能为空")
    private Long entryId;

    @Schema(description = "离职日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "离职日期不能为空")
    private LocalDate leaveDate;

    @Schema(description = "离职原因")
    private String leaveReason;

}
