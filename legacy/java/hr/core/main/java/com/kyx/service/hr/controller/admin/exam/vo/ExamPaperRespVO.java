package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 考试试卷 Response VO")
@Data
public class ExamPaperRespVO {

    @Schema(description = "试卷ID")
    private Long id;

    @Schema(description = "试卷名称")
    private String name;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "题目列表")
    private List<ExamPaperItemRespVO> items;

    @Schema(description = "管理后台 - 考试试卷题目 Response VO")
    @Data
    public static class ExamPaperItemRespVO {

        @Schema(description = "题目ID")
        private Long id;

        @Schema(description = "题目标题")
        private String title;

        @Schema(description = "题型(single/multi/judge/blank/short)")
        private String itemType;

        @Schema(description = "选项JSON")
        private String optionsJson;

        @Schema(description = "分值")
        private Integer score;

        @Schema(description = "填空数量")
        private Integer blankCount;

        @Schema(description = "已保存答案文本")
        private String answerText;

        @Schema(description = "已保存答案JSON")
        private String answerJson;
    }
}
