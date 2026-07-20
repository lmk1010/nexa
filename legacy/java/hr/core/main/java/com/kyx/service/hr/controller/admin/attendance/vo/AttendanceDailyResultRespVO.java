package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 考勤每日结果 Response VO")
@Data
public class AttendanceDailyResultRespVO {

    @Schema(description = "结果ID")
    private Long id;

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "员工姓名")
    private String profileName;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "考勤日期")
    private LocalDate attendanceDate;

    @Schema(description = "应上班时间")
    private LocalDateTime expectedStartTime;

    @Schema(description = "应下班时间")
    private LocalDateTime expectedEndTime;

    @Schema(description = "实际上班时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际下班时间")
    private LocalDateTime actualEndTime;

    @Schema(description = "结果状态")
    private String resultStatus;

    @Schema(description = "迟到分钟")
    private Integer lateMinutes;

    @Schema(description = "早退分钟")
    private Integer earlyLeaveMinutes;

    @Schema(description = "旷工小时")
    private BigDecimal absentHours;

    @Schema(description = "请假小时")
    private BigDecimal leaveHours;

    @Schema(description = "出差小时")
    private BigDecimal tripHours;

    @Schema(description = "来源数据")
    private String sourceJson;

    @Schema(description = "计算时间")
    private LocalDateTime calculatedTime;

}
