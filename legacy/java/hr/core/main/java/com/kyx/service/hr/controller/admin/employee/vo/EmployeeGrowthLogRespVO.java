package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工成长记录 Response VO")
@Data
public class EmployeeGrowthLogRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long profileId;

    @Schema(description = "事件类型：1入职 2转正 3晋升 4降级 5调岗 6离职 7复职 8其他")
    private Integer eventType;

    @Schema(description = "事件日期")
    private LocalDate eventDate;

    @Schema(description = "事件标题")
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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
