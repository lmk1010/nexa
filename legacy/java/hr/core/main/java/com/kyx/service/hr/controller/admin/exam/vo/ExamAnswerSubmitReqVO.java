package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 考试提交 Request VO")
@Data
public class ExamAnswerSubmitReqVO {

    @Schema(description = "作答ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "作答ID不能为空")
    private Long attemptId;

    @Schema(description = "考试ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "考试ID不能为空")
    private Long examId;

    @Schema(description = "试卷ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "试卷ID不能为空")
    private Long paperId;

    @Schema(description = "答案列表")
    private List<Answer> answers;

    @Schema(description = "管理后台 - 考试答案")
    @Data
    public static class Answer {
        @Schema(description = "题目ID")
        private Long itemId;
        @Schema(description = "答案文本")
        private String answerText;
        @Schema(description = "答案JSON")
        private String answerJson;
    }

}
