package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 考试发布保存 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 考试发布保存 VO")
@Data
public class ExamPublishSaveReqVO {

    @Schema(description = "发布ID")
    private Long id;

    @Schema(description = "考试模板ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "考试模板ID不能为空")
    private Long examId;

    @Schema(description = "发布类型(0一次性 1定期)")
    private Integer publishType;

    @Schema(description = "发布范围JSON")
    private String publishScopeJson;

    @Schema(description = "发送方式(0立即 1定时)")
    private Integer sendType;

    @Schema(description = "定时发送时间")
    private LocalDateTime sendAt;

    @Schema(description = "定期规则JSON")
    private String repeatRuleJson;

    @Schema(description = "结束方式(0永不 1指定日期 2指定次数)")
    private Integer repeatEndType;

    @Schema(description = "结束日期")
    private LocalDateTime repeatEndAt;

    @Schema(description = "结束次数")
    private Integer repeatEndCount;

    @Schema(description = "考试开始时间")
    private LocalDateTime startAt;

    @Schema(description = "考试截止时间")
    private LocalDateTime endAt;

    @Schema(description = "考试时长(分钟)")
    private Integer durationMin;

    @Schema(description = "最大作答次数")
    private Integer maxAttempts;

    @Schema(description = "判分模式(AI/MANUAL)")
    private String gradeMode;

    @Schema(description = "提醒规则JSON")
    private String remindRuleJson;

    @Schema(description = "状态(0未发布 1进行中 2已截止 3已暂停)")
    private Integer status;

}
