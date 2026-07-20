package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 员工统计 Response VO")
@Data
public class EmployeeStatisticsRespVO {

    @Schema(description = "总员工数")
    private Integer total;

    @Schema(description = "在职员工数")
    private Integer active;

    @Schema(description = "全职员工数")
    private Integer fullTime;

    @Schema(description = "兼职员工数")
    private Integer partTime;

    @Schema(description = "实习生数")
    private Integer intern;

    @Schema(description = "试用期员工数")
    private Integer probation;

    @Schema(description = "正式员工数")
    private Integer formal;

    @Schema(description = "待离职员工数")
    private Integer leaving;

    @Schema(description = "劳务派遣员工数")
    private Integer labor;

    @Schema(description = "待入职员工数")
    private Integer onboarding;
}
