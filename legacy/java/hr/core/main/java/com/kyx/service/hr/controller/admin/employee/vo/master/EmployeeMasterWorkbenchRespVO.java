package com.kyx.service.hr.controller.admin.employee.vo.master;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 员工主数据工作台 Response VO")
@Data
public class EmployeeMasterWorkbenchRespVO {

    @Schema(description = "员工档案总数")
    private Integer totalProfiles;

    @Schema(description = "正常档案数")
    private Integer activeProfiles;

    @Schema(description = "在职任职记录数")
    private Integer activeEntries;

    @Schema(description = "数据质量分")
    private Integer dataQualityScore;

    @Schema(description = "缺手机号数量")
    private Integer missingMobileCount;

    @Schema(description = "缺账号绑定数量")
    private Integer missingUserCount;

    @Schema(description = "缺部门数量")
    private Integer missingDeptCount;

    @Schema(description = "缺钉钉绑定数量")
    private Integer missingDingTalkCount;

    @Schema(description = "缺必填扩展字段数量")
    private Integer missingCustomFieldCount;

    @Schema(description = "重复手机号涉及人数")
    private Integer duplicateMobileCount;

    @Schema(description = "重复身份证涉及人数")
    private Integer duplicateIdNumberCount;

    @Schema(description = "30 天内合同到期人数")
    private Integer contractExpiringCount;

    @Schema(description = "30 天内待转正人数")
    private Integer probationDueCount;

    @Schema(description = "优先处理问题")
    private List<EmployeeDataQualityIssueRespVO> topIssues;

}
