package com.kyx.service.biz.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Schema(description = "Admin - Work Requirement action Request VO")
@Data
public class WorkRequirementActionReqVO {

    @Schema(description = "Requirement ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "Requirement ID is required")
    private Long id;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Attachment URLs")
    private List<String> attachmentUrls;

    @Schema(description = "BPM start user selected assignees, key is task id")
    private Map<String, List<Long>> startUserSelectAssignees;

}
