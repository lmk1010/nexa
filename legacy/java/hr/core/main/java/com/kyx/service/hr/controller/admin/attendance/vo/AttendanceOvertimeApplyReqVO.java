package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "Admin - Attendance overtime apply Request VO")
@Data
public class AttendanceOvertimeApplyReqVO {

    @Schema(description = "Profile id, optional for self apply")
    private Long profileId;

    @Schema(description = "User id, optional for self apply")
    private Long userId;

    @Schema(description = "Overtime type: WORKDAY/WEEKEND/HOLIDAY")
    private String overtimeType;

    @Schema(description = "Start time", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "加班开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "End time", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "加班结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "Convert approved overtime to compensatory leave")
    private Boolean convertToLeave;

    @Schema(description = "Leave type code")
    private String leaveTypeCode;

    @Schema(description = "Reason")
    private String reason;

}
