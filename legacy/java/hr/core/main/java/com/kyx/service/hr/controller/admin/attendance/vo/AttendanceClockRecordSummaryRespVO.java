package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Admin - attendance summary response")
@Data
public class AttendanceClockRecordSummaryRespVO {

    @Schema(description = "Record count")
    private Long recordCount;

    @Schema(description = "User count")
    private Long userCount;

    @Schema(description = "Attendance day count")
    private Long dayCount;

    @Schema(description = "Normal count")
    private Long normalCount;

    @Schema(description = "Late count")
    private Long lateCount;

    @Schema(description = "Early count")
    private Long earlyCount;

    @Schema(description = "Absenteeism count")
    private Long absenteeismCount;

    @Schema(description = "Unknown count")
    private Long unknownCount;

    @Schema(description = "Leave covered abnormal count")
    private Long leaveCoveredCount;

    @Schema(description = "Clock in count")
    private Long clockInCount;

    @Schema(description = "Clock out count")
    private Long clockOutCount;

    @Schema(description = "Manual source count")
    private Long manualCount;

    @Schema(description = "DingTalk source count")
    private Long dingTalkCount;

}
