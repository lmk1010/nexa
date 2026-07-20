package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.time.LocalDateTime;

/**
 * 问卷分配 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷分配 Response VO")
@Data
public class QuestionnaireAssignmentRespVO {

    @Schema(description = "分配ID")
    private Long id;

    @Schema(description = "问卷ID")
    private Long questionnaireId;

    @Schema(description = "发布ID")
    private Long publishId;

    @Schema(description = "批次号")
    private Integer batchNo;

    @Schema(description = "批次标签")
    private String batchLabel;

    @Schema(description = "批次开始时间")
    private LocalDateTime batchStartAt;

    @Schema(description = "批次截止时间")
    private LocalDateTime batchEndAt;

    @Schema(description = "问卷名称")
    private String questionnaireName;

    @Schema(description = "问卷类型")
    private String questionnaireType;

    @Schema(description = "截止时间")
    private LocalDateTime deadlineAt;

    @Schema(description = "评价人ID")
    private Long evaluatorId;

    @Schema(description = "评价人姓名")
    private String evaluatorName;

    @Schema(description = "被评人ID")
    private Long targetId;

    @Schema(description = "被评人姓名")
    private String targetName;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "提交时间")
    private Date submitTime;

    @Schema(description = "总分")
    private Integer totalScore;

}
