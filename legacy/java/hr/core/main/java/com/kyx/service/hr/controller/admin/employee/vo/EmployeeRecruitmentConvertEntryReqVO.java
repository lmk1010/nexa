package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "管理后台 - 招聘候选人转入职 Request VO")
@Data
public class EmployeeRecruitmentConvertEntryReqVO {

    @Schema(description = "招聘记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "招聘记录ID不能为空")
    private Long id;

    @Schema(description = "入职日期")
    private LocalDate entryDate;

    @Schema(description = "入职流程类型（1简易入职 2审批入职）")
    private Integer processType;

    @Schema(description = "入职状态（1待提交 2审批中 3已通过 4已拒绝 5已取消）")
    private Integer onboardingStatus;

    @Schema(description = "工作状态（0待填写 1待入职 2试用期 3在职 4离职）")
    private Integer workStatus;

    @Schema(description = "备注")
    private String remark;
}
