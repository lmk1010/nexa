package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 问卷答案 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷答案 Response VO")
@Data
public class QuestionnaireAnswerRespVO {

    @Schema(description = "题目ID")
    private Long itemId;

    @Schema(description = "题目标题")
    private String itemTitle;

    @Schema(description = "文本答案")
    private String answerText;

    @Schema(description = "分值答案")
    private Integer answerScore;

    @Schema(description = "JSON答案")
    private String answerJson;

}
