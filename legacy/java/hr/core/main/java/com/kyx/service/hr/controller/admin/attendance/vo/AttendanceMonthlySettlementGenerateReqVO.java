package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 考勤月结生成 Request VO")
@Data
public class AttendanceMonthlySettlementGenerateReqVO {

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "部门ID，预留")
    private Long deptId;

}
