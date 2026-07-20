package com.kyx.service.hr.controller.admin.attendance.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AttendanceDailyResultSummaryRespVO {

    private Integer totalCount = 0;

    private Integer normalCount = 0;

    private Integer leaveCount = 0;

    private Integer tripCount = 0;

    private Integer abnormalCount = 0;

    private Integer missingCount = 0;

    private Integer absenteeismCount = 0;

    private Integer lateCount = 0;

    private Integer earlyCount = 0;

    private BigDecimal leaveHours = BigDecimal.ZERO;

    private BigDecimal tripHours = BigDecimal.ZERO;

    private BigDecimal absentHours = BigDecimal.ZERO;

    private BigDecimal abnormalRate = BigDecimal.ZERO;

}
