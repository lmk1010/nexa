package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 月度考勤确认 Response VO")
@Data
public class AttendanceMonthlyConfirmRespVO {

    private Long id;

    private Long settlementId;

    private String settlementMonth;

    private Long deptId;

    private Long profileId;

    private String profileName;

    private Long userId;

    private String userNickname;

    private String status;

    private LocalDateTime confirmedTime;

    private LocalDateTime issueTime;

    private String issueRemark;

    private LocalDateTime resolvedTime;

    private Long resolvedBy;

    private String resolvedByName;

    private String resolveRemark;

}
