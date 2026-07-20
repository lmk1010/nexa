package com.kyx.service.hr.controller.admin.employee.vo.master;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 员工数据质量 Response VO")
@Data
public class EmployeeDataQualityRespVO {

    @Schema(description = "总问题数")
    private Integer totalIssueCount;

    @Schema(description = "高风险问题数")
    private Integer highRiskCount;

    @Schema(description = "中风险问题数")
    private Integer mediumRiskCount;

    @Schema(description = "低风险问题数")
    private Integer lowRiskCount;

    @Schema(description = "数据质量分")
    private Integer score;

    @Schema(description = "问题明细")
    private List<EmployeeDataQualityIssueRespVO> issues;

}
