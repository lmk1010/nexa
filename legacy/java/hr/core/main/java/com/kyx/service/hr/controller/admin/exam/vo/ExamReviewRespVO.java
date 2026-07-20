package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 考试答案回看 Response VO")
@Data
public class ExamReviewRespVO {

    @Schema(description = "是否允许查看标准答案")
    private Boolean canReview;

    @Schema(description = "不可查看原因")
    private String message;

    @Schema(description = "应参加人数")
    private Integer participantCount;

    @Schema(description = "已提交人数")
    private Integer submittedCount;

    @Schema(description = "作答ID")
    private Long attemptId;

    @Schema(description = "试卷ID")
    private Long paperId;

    @Schema(description = "试卷名称")
    private String paperName;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "本次得分")
    private Integer userScore;

    @Schema(description = "提交时间")
    private LocalDateTime submitAt;

    @Schema(description = "题目列表")
    private List<ItemRespVO> items;

    @Data
    public static class ItemRespVO {

        private Long itemId;

        private Integer sortNo;

        private String title;

        private String itemType;

        private String optionsJson;

        private String standardAnswerJson;

        private String answerText;

        private String answerJson;

        private Integer answerScore;

        private Integer maxScore;

        private String aiComment;

    }

}
