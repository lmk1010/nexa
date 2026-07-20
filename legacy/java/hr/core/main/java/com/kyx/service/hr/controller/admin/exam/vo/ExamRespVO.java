package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 考试 Response VO")
@Data
public class ExamRespVO {

    @Schema(description = "考试ID")
    private Long id;

    @Schema(description = "考试编码")
    private String code;

    @Schema(description = "考试名称")
    private String name;

    @Schema(description = "考试类型(0一次性 1定期考核)")
    private Integer examType;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "考试模式")
    private String examMode;

    @Schema(description = "考试时长(分钟)")
    private Integer durationMin;

    @Schema(description = "及格分")
    private Integer passScore;

    @Schema(description = "最大次数")
    private Integer maxAttempts;

    @Schema(description = "开始时间")
    private LocalDateTime startAt;

    @Schema(description = "结束时间")
    private LocalDateTime endAt;

    @Schema(description = "发布范围JSON")
    private String publishScopeJson;

    @Schema(description = "试卷列表")
    private List<Paper> papers;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "租户名称")
    private String tenantName;

    @Data
    public static class Paper {
        private Long id;
        private String name;
        private String version;
        private Integer totalScore;
        private String ruleJson;
        private List<Item> items;
    }

    @Data
    public static class Item {
        private Long id;
        private Integer sortNo;
        private String title;
        private String itemType;
        private String optionsJson;
        private String answerJson;
        private Integer score;
        private Boolean required;
    }
}
