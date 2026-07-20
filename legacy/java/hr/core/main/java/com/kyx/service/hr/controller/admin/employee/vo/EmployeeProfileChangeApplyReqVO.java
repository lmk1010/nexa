package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "Admin - Employee profile change apply Request VO")
@Data
public class EmployeeProfileChangeApplyReqVO {

    @Schema(description = "Profile id, optional for self apply")
    private Long profileId;

    @Schema(description = "User id, optional for self apply")
    private Long userId;

    @Schema(description = "Change type")
    private String changeType;

    @Schema(description = "New profile fields json", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "变更内容不能为空")
    private String afterJson;

    @Schema(description = "Change reason")
    private String reason;

}
