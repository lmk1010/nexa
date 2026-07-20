package com.kyx.service.biz.controller.admin.work.vo;

import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.biz.enums.WorkRequirementPriorityEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Schema(description = "Admin - Work Requirement create/update Request VO")
@Data
public class WorkRequirementSaveReqVO {

    @Schema(description = "Requirement ID", example = "1")
    private Long id;

    @Schema(description = "Parent requirement ID, present when creating sub-requirement", example = "100")
    private Long parentId;

    @Schema(description = "Title", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Title is required")
    private String title;

    @Schema(description = "Description", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Description is required")
    private String description;

    @Schema(description = "Priority", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "Priority is required")
    @InEnum(WorkRequirementPriorityEnum.class)
    private Integer priority;

    @Schema(description = "Expected finish date")
    private Date expectedFinishDate;

    @Schema(description = "Estimated user count", example = "100")
    @Min(value = 1, message = "预计使用人数不能小于 1")
    private Integer estimatedUserCount;

    @Schema(description = "Assignee user ID", example = "1024")
    private Long assigneeUserId;

    @Schema(description = "Assignee tenant ID", example = "171")
    private Long assigneeTenantId;

    @Schema(description = "Assignee display name", example = "张三")
    private String assigneeName;

    @Schema(description = "Collaborator developer user IDs")
    private List<Long> collaboratorUserIds;

    @Schema(description = "Proposer name fallback", example = "张三")
    private String proposerName;

    @Schema(description = "Proposer dept fallback", example = "研发部")
    private String proposerDept;

    @Schema(description = "Target dept", example = "技术部")
    private String targetDept;

    @Schema(description = "Attachment image urls")
    private List<String> attachmentUrls;

    @Schema(description = "Integral", example = "10.00")
    private BigDecimal integral;

    @Schema(description = "Use type", example = "线上使用")
    private String useType;

    @Schema(description = "Append remark")
    private String remark;

    @Schema(description = "BPM start user selected assignees, key is task id")
    private Map<String, List<Long>> startUserSelectAssignees;

}
