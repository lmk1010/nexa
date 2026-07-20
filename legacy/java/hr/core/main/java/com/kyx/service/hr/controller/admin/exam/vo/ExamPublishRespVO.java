package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试发布 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 考试发布 Response VO")
@Data
public class ExamPublishRespVO {

    @Schema(description = "发布ID")
    private Long id;

    @Schema(description = "考试模板ID")
    private Long examId;

    @Schema(description = "考试名称")
    private String examName;

    @Schema(description = "绉熸埛ID")
    private Long tenantId;

    @Schema(description = "绉熸埛鍚嶇О")
    private String tenantName;

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

    @Schema(description = "结束方式")
    private Integer repeatEndType;

    @Schema(description = "结束日期")
    private LocalDateTime repeatEndAt;

    @Schema(description = "结束次数")
    private Integer repeatEndCount;

    @Schema(description = "下次发布时间")
    private LocalDateTime nextPublishAt;

    @Schema(description = "已生成批次数")
    private Integer generatedCount;

    @Schema(description = "父发布ID")
    private Long parentPublishId;

    @Schema(description = "批次号")
    private Integer batchNo;

    @Schema(description = "批次标签")
    private String batchLabel;

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

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "参与人数")
    private Integer participantCount;

    @Schema(description = "已提交人数")
    private Integer submittedCount;

    @Schema(description = "通过率")
    private Double passRate;

}
