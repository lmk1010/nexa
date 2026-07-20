package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 问卷 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷 Response VO")
@Data
public class QuestionnaireRespVO {

    @Schema(description = "问卷ID")
    private Long id;

    @Schema(description = "问卷编码")
    private String code;

    @Schema(description = "问卷名称")
    private String name;

    @Schema(description = "问卷类型")
    private String type;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "适用角色")
    private String roleScope;

    @Schema(description = "被评对象规则(JSON)")
    private String targetRuleJson;

    @Schema(description = "周期开始")
    private LocalDate periodStart;

    @Schema(description = "周期结束")
    private LocalDate periodEnd;

    @Schema(description = "题目列表")
    private List<Item> items;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "题目数")
    private Integer questionCount;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "题目 Response VO")
    @Data
    public static class Item {
        @Schema(description = "题目ID")
        private Long id;
        @Schema(description = "题目序号")
        private Integer sortNo;
        @Schema(description = "题目标题")
        private String title;
        @Schema(description = "题型")
        private String itemType;
        @Schema(description = "是否必填")
        private Boolean required;
        @Schema(description = "最大分")
        private Integer maxScore;
        @Schema(description = "选项列表")
        private List<Option> options;
    }

    @Schema(description = "选项 Response VO")
    @Data
    public static class Option {
        @Schema(description = "选项ID")
        private Long id;
        @Schema(description = "选项序号")
        private Integer sortNo;
        @Schema(description = "选项内容")
        private String optionText;
        @Schema(description = "选项分值")
        private Integer optionScore;
    }

}
