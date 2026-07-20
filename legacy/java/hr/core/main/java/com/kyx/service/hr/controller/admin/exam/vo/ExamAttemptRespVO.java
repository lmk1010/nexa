package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 考试作答记录 Response VO")
@Data
public class ExamAttemptRespVO {

    @Schema(description = "作答ID")
    private Long id;

    @Schema(description = "考试ID")
    private Long examId;

    @Schema(description = "发布ID")
    private Long publishId;

    @Schema(description = "试卷ID")
    private Long paperId;

    @Schema(description = "答题人ID")
    private Long userId;

    @Schema(description = "开始时间")
    private LocalDateTime startAt;

    @Schema(description = "提交时间")
    private LocalDateTime submitAt;

    @Schema(description = "状态(0进行中 1已提交)")
    private Integer status;

    @Schema(description = "总分")
    private Integer totalScore;

    private String gradeMode;

    private Integer gradeStatus;

    private String gradeError;

}
