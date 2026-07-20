package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工作台员工概览 Response VO")
@Data
public class EmployeeWorkspaceOverviewRespVO {

    @Schema(description = "员工统计")
    private EmployeeStatisticsRespVO statistics;

    @Schema(description = "员工总览")
    private EmployeeOverviewRespVO overview;

    @Schema(description = "员工趋势")
    private EmployeeStatisticsTrendRespVO trend;

}
