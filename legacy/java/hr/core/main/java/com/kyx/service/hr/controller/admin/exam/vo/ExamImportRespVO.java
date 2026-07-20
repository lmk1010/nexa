package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 考试导入 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 考试导入 Response VO")
@Data
public class ExamImportRespVO {

    @Schema(description = "试卷列表")
    private List<Paper> papers;

    @Data
    public static class Paper {
        private String name;
        private String version;
        private Integer totalScore;
        private String ruleJson;
        private List<Item> items;
    }

    @Data
    public static class Item {
        private String title;
        private String itemType;
        private Integer score;
        private Boolean required;
        private Integer sortNo;
        private String optionsJson;
        private String answerJson;
        private List<Option> options;
    }

    @Data
    public static class Option {
        private String text;
        private Boolean isCorrect;
    }
}
