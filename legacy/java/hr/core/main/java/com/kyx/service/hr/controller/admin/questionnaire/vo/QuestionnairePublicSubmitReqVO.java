package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 公开问卷提交 VO
 */
@Schema(description = "公开问卷提交 VO")
@Data
public class QuestionnairePublicSubmitReqVO {

    @Schema(description = "访问令牌", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "访问令牌不能为空")
    private String token;

    @Schema(description = "访问密码")
    private String password;

    @Schema(description = "填写人姓名")
    private String respondentName;

    @Schema(description = "填写人手机")
    private String respondentPhone;

    @Schema(description = "填写人邮箱")
    private String respondentEmail;

    @Schema(description = "答案列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "答案不能为空")
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        @Schema(description = "题目ID")
        private Long itemId;

        @Schema(description = "文本答案")
        private String answerText;

        @Schema(description = "评分")
        private Integer answerScore;

        @Schema(description = "JSON答案")
        private String answerJson;
    }

}
