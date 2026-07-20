package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Schema(description = "管理后台 - 考勤班次规则 Response VO")
@Data
public class AttendanceShiftRuleRespVO {

    private Long id;

    private String shiftName;

    private LocalTime startTime;

    private LocalTime endTime;

    private LocalTime restStartTime;

    private LocalTime restEndTime;

    private Integer lateGraceMinutes;

    private Integer earlyLeaveGraceMinutes;

    private BigDecimal workHours;

    private Boolean defaultFlag;

    private Integer status;

    private String remark;

}
