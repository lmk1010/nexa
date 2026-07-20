package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 我的月度考勤日历项 Response VO")
@Data
public class AttendanceMyMonthDayRespVO {

    @Schema(description = "考勤日期")
    private LocalDate attendanceDate;

    @Schema(description = "是否已上班打卡")
    private Boolean hasClockIn;

    @Schema(description = "是否已下班打卡")
    private Boolean hasClockOut;

    @Schema(description = "当日打卡次数")
    private Integer recordCount;

    @Schema(description = "首次上班打卡时间")
    private LocalDateTime firstClockInTime;

    @Schema(description = "末次下班打卡时间")
    private LocalDateTime lastClockOutTime;

    @Schema(description = "请假分钟数")
    private Integer leaveMinutes;

    @Schema(description = "日状态：COMPLETE/MISSING_IN/MISSING_OUT/LEAVE")
    private String dayStatus;

}
