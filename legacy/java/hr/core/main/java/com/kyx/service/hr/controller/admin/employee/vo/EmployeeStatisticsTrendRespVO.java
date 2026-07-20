package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 员工统计趋势 Response VO")
@Data
public class EmployeeStatisticsTrendRespVO {

    @Schema(description = "月份标签（从旧到新）")
    private List<String> months;

    @Schema(description = "员工总数趋势")
    private List<Integer> totalTrend;

    @Schema(description = "在职员工趋势")
    private List<Integer> activeTrend;

    @Schema(description = "试用期员工趋势")
    private List<Integer> probationTrend;

    @Schema(description = "全职员工趋势")
    private List<Integer> fullTimeTrend;

    @Schema(description = "兼职员工趋势")
    private List<Integer> partTimeTrend;

    @Schema(description = "正式员工趋势")
    private List<Integer> formalTrend;

    @Schema(description = "离职办理中趋势")
    private List<Integer> leavingTrend;

    @Schema(description = "入职办理中趋势")
    private List<Integer> onboardingTrend;

    @Schema(description = "平均年龄趋势")
    private List<Double> avgAgeTrend;

    @Schema(description = "稳定性指数趋势")
    private List<Integer> stabilityTrend;
}
