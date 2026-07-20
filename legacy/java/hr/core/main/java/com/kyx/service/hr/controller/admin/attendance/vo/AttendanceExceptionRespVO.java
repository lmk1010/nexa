package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 考勤异常 Response VO")
@Data
public class AttendanceExceptionRespVO {

    @Schema(description = "异常ID")
    private Long id;

    @Schema(description = "每日结果ID")
    private Long dailyResultId;

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

    @Schema(description = "异常类型")
    private String exceptionType;

    @Schema(description = "异常状态")
    private String exceptionStatus;

    @Schema(description = "异常原因")
    private String reason;

    @Schema(description = "处理人")
    private Long handlerId;

    @Schema(description = "处理人名称")
    private String handlerName;

    @Schema(description = "处理时间")
    private LocalDateTime handledTime;

    @Schema(description = "处理备注")
    private String handleRemark;

}
