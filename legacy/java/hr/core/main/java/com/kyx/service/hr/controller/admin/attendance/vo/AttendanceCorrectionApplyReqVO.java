package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "Admin - Attendance correction apply Request VO")
@Data
public class AttendanceCorrectionApplyReqVO {

    @Schema(description = "Profile id, optional for self apply")
    private Long profileId;

    @Schema(description = "User id, optional for self apply")
    private Long userId;

    @Schema(description = "Apply type: CORRECTION/FIELD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "申请类型不能为空")
    private String applyType;

    @Schema(description = "Clock type: IN/OUT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "打卡类型不能为空")
    private String clockType;

    @Schema(description = "Correction clock time", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "补卡时间不能为空")
    private LocalDateTime clockTime;

    @Schema(description = "Reason")
    private String reason;

    @Schema(description = "Location name")
    private String locationName;

    @Schema(description = "Location address")
    private String locationAddress;

    @Schema(description = "Attachment JSON")
    private String attachmentJson;

    @Schema(description = "Related attendance exception id")
    private Long exceptionId;

}
