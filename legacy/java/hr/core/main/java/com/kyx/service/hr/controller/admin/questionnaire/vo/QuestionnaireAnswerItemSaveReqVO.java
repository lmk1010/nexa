package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 问卷答案项保存 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷答案项保存 VO")
@Data
public class QuestionnaireAnswerItemSaveReqVO {

    @Schema(description = "题目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long itemId;

    @Schema(description = "文本答案")
    private String answerText;

    @Schema(description = "得分")
    private Integer answerScore;

    @Schema(description = "答案JSON")
    private String answerJson;

}
