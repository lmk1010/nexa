package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Admin - Attendance correction Response VO")
@Data
public class AttendanceCorrectionRespVO {

    private Long id;

    private Long profileId;

    private String profileName;

    private Long userId;

    private String userNickname;

    private LocalDate attendanceDate;

    private String applyType;

    private String clockType;

    private LocalDateTime clockTime;

    private String reason;

    private String locationName;

    private String locationAddress;

    private String attachmentJson;

    private String status;

    private String processInstanceId;

    private Long approverId;

    private String approverNickname;

    private LocalDateTime approvedTime;

    private String approveRemark;

    private Long clockRecordId;

    private Long exceptionId;

}
