package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 问卷结果 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷结果 Response VO")
@Data
public class QuestionnaireResultRespVO {

    @Schema(description = "被评人ID")
    private Long targetId;

    @Schema(description = "被评人姓名")
    private String targetName;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "总分")
    private Double totalScore;

    @Schema(description = "平均分")
    private Double avgScore;

    @Schema(description = "已评人数")
    private Integer assignmentCount;

    @Schema(description = "应评人数")
    private Integer totalAssignmentCount;

    @Schema(description = "未评人数")
    private Integer pendingCount;

    @Schema(description = "折算分（未填按0）")
    private Double avgScoreWithPendingAsZero;

}
