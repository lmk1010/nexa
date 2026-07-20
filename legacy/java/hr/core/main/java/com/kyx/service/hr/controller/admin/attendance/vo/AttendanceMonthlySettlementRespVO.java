package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 考勤月结 Response VO")
@Data
public class AttendanceMonthlySettlementRespVO {

    private Long id;

    private String settlementMonth;

    private Long deptId;

    private String status;

    private LocalDateTime generatedTime;

    private LocalDateTime lockedTime;

    private Long lockedBy;

    private String summaryJson;

}
