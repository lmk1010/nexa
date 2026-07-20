package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工成长记录新增/修改 Request VO")
@Data
public class EmployeeGrowthLogSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工档案ID不能为空")
    private Long profileId;

    @Schema(description = "事件类型：1入职 2转正 3晋升 4降级 5调岗 6离职 7复职 8其他", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "事件类型不能为空")
    private Integer eventType;

    @Schema(description = "事件日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "事件日期不能为空")
    private LocalDate eventDate;

    @Schema(description = "事件标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "事件标题不能为空")
    private String title;

    @Schema(description = "事件描述")
    private String content;

    @Schema(description = "变动前部门ID")
    private Long beforeDeptId;

    @Schema(description = "变动前部门名称")
    private String beforeDeptName;

    @Schema(description = "变动前职位")
    private String beforeJobTitle;

    @Schema(description = "变动前职级ID")
    private Long beforeJobLevelId;

    @Schema(description = "变动前职级名称")
    private String beforeJobLevelName;

    @Schema(description = "变动后部门ID")
    private Long afterDeptId;

    @Schema(description = "变动后部门名称")
    private String afterDeptName;

    @Schema(description = "变动后职位")
    private String afterJobTitle;

    @Schema(description = "变动后职级ID")
    private Long afterJobLevelId;

    @Schema(description = "变动后职级名称")
    private String afterJobLevelName;

    @Schema(description = "审批状态：0待审批 1已通过 2已拒绝")
    private Integer approvalStatus;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批人姓名")
    private String approverName;

    @Schema(description = "审批时间")
    private LocalDateTime approvalTime;

    @Schema(description = "审批备注")
    private String approvalRemark;

    @Schema(description = "附件URL列表(JSON)")
    private String attachmentUrls;

}
