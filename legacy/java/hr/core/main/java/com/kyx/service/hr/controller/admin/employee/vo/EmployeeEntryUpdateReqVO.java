package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "管理后台 - 员工入职记录局部更新 Request VO")
@Data
public class EmployeeEntryUpdateReqVO {

    @Schema(description = "入职记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入职记录ID不能为空")
    private Long id;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "职位")
    private String jobTitle;

    @Schema(description = "分机号")
    private String extension;

    @Schema(description = "办公地点")
    private String officeLocation;

    @Schema(description = "职级ID")
    private Long jobLevelId;

    @Schema(description = "序列ID")
    private Long jobSequenceId;

    @Schema(description = "工作地点ID")
    private Long workLocationId;

    @Schema(description = "直属上级ID")
    private Long directSupervisorId;

    @Schema(description = "用工类型（1全职 2兼职 3劳务 4实习）")
    private Integer employmentType;

    @Schema(description = "试用期月数")
    private Integer probationMonths;

    @Schema(description = "合同类型（1劳动合同 2劳务合同 3实习协议）")
    private Integer contractType;

    @Schema(description = "合同开始日期")
    private LocalDate contractStartDate;

    @Schema(description = "合同结束日期")
    private LocalDate contractEndDate;

    @Schema(description = "工作状态（0待填写 1待入职 2试用期 3在职 4离职）")
    private Integer workStatus;

    @Schema(description = "离职日期")
    private LocalDate leaveDate;

    @Schema(description = "离职原因")
    private String leaveReason;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "银行分支")
    private String bankBranch;

    @Schema(description = "银行账户")
    private String bankAccount;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "备注")
    private String remark;
}
