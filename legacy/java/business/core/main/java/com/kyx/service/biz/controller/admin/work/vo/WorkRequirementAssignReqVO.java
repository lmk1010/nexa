package com.kyx.service.biz.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Admin - Work Requirement assign Request VO")
@Data
public class WorkRequirementAssignReqVO {

    @Schema(description = "Requirement ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "Requirement ID is required")
    private Long id;

    @Schema(description = "Assignee user ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "Assignee user ID is required")
    private Long assigneeUserId;

    @Schema(description = "Assignee tenant ID", example = "171")
    private Long assigneeTenantId;

    @Schema(description = "Assignee display name", example = "张三")
    private String assigneeName;

    @Schema(description = "Collaborator developer user IDs")
    private List<Long> collaboratorUserIds;

}
