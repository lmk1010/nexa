package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工业绩信息新增/修改 Request VO")
@Data
public class EmployeePerformanceSaveReqVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工档案ID不能为空")
    private Long profileId;

    @Schema(description = "绩效方案ID")
    private Long schemeId;

    @Schema(description = "绩效方案编码")
    private String schemeCode;

    @Schema(description = "绩效方案名称")
    private String schemeName;

    @Schema(description = "绩效方案类型")
    private String schemeType;

    @Schema(description = "绩效方案周期类型")
    private String cycleType;

    @Schema(description = "考核周期")
    private String period;

    @Schema(description = "考核得分")
    private BigDecimal score;

    @Schema(description = "考核等级")
    private String grade;

    @Schema(description = "考核结果")
    private String result;

    @Schema(description = "考核日期")
    private LocalDate evaluatedDate;

    @Schema(description = "绩效周期状态")
    private String cycleStatus;

    @Schema(description = "绩效目标")
    private String goalContent;

    @Schema(description = "员工自评")
    private String selfReview;

    @Schema(description = "主管评价")
    private String managerReview;

    @Schema(description = "校准结果")
    private String calibrationResult;

    @Schema(description = "面谈时间")
    private LocalDateTime interviewTime;

    @Schema(description = "下一步跟进时间")
    private LocalDateTime nextFollowTime;

    @Schema(description = "绩效结果应用类型")
    private String applicationType;

    @Schema(description = "绩效结果应用状态")
    private String applicationStatus;

    @Schema(description = "绩效结果应用时间")
    private LocalDateTime applicationTime;

    @Schema(description = "绩效结果应用备注")
    private String applicationRemark;

    @Schema(description = "绩效审批状态")
    private String approvalStatus;

    @Schema(description = "绩效提交审批时间")
    private LocalDateTime submittedTime;

    @Schema(description = "绩效审批人")
    private Long approvedBy;

    @Schema(description = "绩效审批时间")
    private LocalDateTime approvedTime;

    @Schema(description = "绩效审批备注")
    private String approvalRemark;

    @Schema(description = "备注")
    private String remark;
}
