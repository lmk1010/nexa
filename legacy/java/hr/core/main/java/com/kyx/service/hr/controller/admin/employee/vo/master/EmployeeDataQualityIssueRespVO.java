package com.kyx.service.hr.controller.admin.employee.vo.master;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 员工数据质量问题 Response VO")
@Data
public class EmployeeDataQualityIssueRespVO {

    @Schema(description = "问题类型")
    private String issueType;

    @Schema(description = "问题名称")
    private String issueName;

    @Schema(description = "风险等级：HIGH/MEDIUM/LOW")
    private String severity;

    @Schema(description = "员工档案 ID")
    private Long profileId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "部门 ID")
    private Long deptId;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "建议动作")
    private String action;

}
