package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 问卷题目统计 Response VO")
@Data
public class QuestionnaireItemStatRespVO {

    @Schema(description = "题目ID")
    private Long itemId;

    @Schema(description = "题目标题")
    private String title;

    @Schema(description = "题型")
    private String itemType;

    @Schema(description = "选项统计")
    private List<OptionStat> optionStats;

    @Schema(description = "得分平均值")
    private Double avgScore;

    @Schema(description = "得分最小值")
    private Integer minScore;

    @Schema(description = "得分最大值")
    private Integer maxScore;

    @Schema(description = "得分次数")
    private Integer scoreCount;

    @Schema(description = "文本数量")
    private Integer textCount;

    @Schema(description = "文本样例")
    private List<String> textSamples;

    @Schema(description = "选项统计")
    @Data
    public static class OptionStat {
        @Schema(description = "选项内容")
        private String optionText;
        @Schema(description = "次数")
        private Integer count;
    }
}
