package com.kyx.service.biz.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Admin - Work Requirement comment create Request VO")
@Data
public class WorkRequirementCommentCreateReqVO {

    @Schema(description = "Requirement ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "Requirement ID is required")
    private Long requirementId;

    @Schema(description = "Comment type", example = "COMMENT")
    private String commentType;

    @Schema(description = "Content")
    private String content;

    @Schema(description = "Attachment urls")
    private List<String> attachmentUrls;

    @Schema(description = "Target user ID", example = "2")
    private Long targetUserId;

}
