package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "管理后台 - 员工入职记录新增/修改 Request VO")
@Data
public class EmployeeEntrySaveReqVO {

    @Schema(description = "入职记录ID")
    private Long id;

    @Schema(description = "入职编号")
    private String entryNo;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工档案ID不能为空")
    private Long profileId;

    @Schema(description = "员工编号")
    private String employeeNo;

    @Schema(description = "入职类型（1首次入职 2再入职）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入职类型不能为空")
    private Integer entryType;

    @Schema(description = "入职流程类型（1简易入职 2审批入职）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入职流程类型不能为空")
    private Integer processType;

    @Schema(description = "入职日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入职日期不能为空")
    private LocalDate entryDate;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "岗位编号数组")
    private String postIds;

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

    @Schema(description = "工作状态（0待填写 1待入职 2试用期 3在职 4离职）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工作状态不能为空")
    private Integer workStatus;

    @Schema(description = "入职状态（1待提交 2审批中 3已通过 4已拒绝 5已取消）")
    private Integer onboardingStatus;

    @Schema(description = "离职日期")
    private LocalDate leaveDate;

    @Schema(description = "离职原因")
    private String leaveReason;

    @Schema(description = "取消入职原因")
    private String cancelReason;

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
