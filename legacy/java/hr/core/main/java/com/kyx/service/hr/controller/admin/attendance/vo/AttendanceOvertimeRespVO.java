package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Admin - Attendance overtime Response VO")
@Data
public class AttendanceOvertimeRespVO {

    private Long id;

    private Long profileId;

    private String profileName;

    private Long userId;

    private String userNickname;

    private LocalDate overtimeDate;

    private String overtimeType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal durationHours;

    private Boolean convertToLeave;

    private String leaveTypeCode;

    private BigDecimal balanceHours;

    private Boolean balanceSynced;

    private String reason;

    private String status;

    private String processInstanceId;

    private Long approverId;

    private String approverNickname;

    private LocalDateTime approvedTime;

    private String approveRemark;

}
