package com.kyx.service.biz.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Schema(description = "Admin - Work Requirement rate save Request VO")
@Data
public class WorkRequirementRateSaveReqVO {

    @Schema(description = "Requirement ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "Requirement ID is required")
    private Long requirementId;

    @Schema(description = "Score(1-3)", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "Score is required")
    @Min(value = 1, message = "Score must be >= 1")
    @Max(value = 3, message = "Score must be <= 3")
    private Integer score;

    @Schema(description = "Rate content", example = "处理很及时")
    private String content;

    @Schema(description = "Target user ID", example = "2")
    private Long targetUserId;

}
