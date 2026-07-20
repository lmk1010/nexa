package com.kyx.service.hr.controller.admin.exam.vo;

import com.alibaba.excel.annotation.ExcelIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试结果 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 考试结果 Response VO")
@Data
public class ExamResultRespVO {

    @ExcelIgnore
    private Long attemptId;

    @ExcelIgnore
    private Long publishId;

    @Schema(description = "用户ID")
    @ExcelIgnore
    private Long userId;

    private String userName;

    @Schema(description = "总分")
    private Integer totalScore;

    private String gradeMode;

    private Integer gradeStatus;

    private String gradeError;

    @Schema(description = "是否通过")
    private Boolean passFlag;

    @Schema(description = "提交时间")
    private LocalDateTime submitAt;

    @ExcelIgnore
    private java.util.List<AnswerRespVO> answers;

    @Data
    public static class AnswerRespVO {

        private Long itemId;

        private Long answerId;

        private Integer sortNo;

        private String title;

        private String itemType;

        private String optionsJson;

        private String answerText;

        private String answerJson;

        private String standardAnswerJson;

        private Integer maxScore;

        private Integer answerScore;

        private Integer manualScore;

        private String aiComment;

        private Integer gradeStatus;

        private String gradeError;

    }

}
