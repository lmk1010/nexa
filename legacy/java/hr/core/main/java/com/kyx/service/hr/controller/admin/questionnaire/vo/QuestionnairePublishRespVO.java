package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问卷发布 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷发布 Response VO")
@Data
public class QuestionnairePublishRespVO {

    @Schema(description = "发布ID")
    private Long id;

    @Schema(description = "问卷ID")
    private Long questionnaireId;

    @Schema(description = "问卷名称")
    private String questionnaireName;

    @Schema(description = "问卷类型")
    private String questionnaireType;

    @Schema(description = "发布范围(JSON)")
    private String publishScopeJson;

    @Schema(description = "发送方式(0立即 1定时)")
    private Integer sendType;

    @Schema(description = "周期类型: 0一次性 1每天 2每周 3每月")
    private Integer scheduleType;

    @Schema(description = "每周几(1-7, 1=周一)")
    private Integer scheduleDayOfWeek;

    @Schema(description = "每月几号(1-31)")
    private Integer scheduleDayOfMonth;

    @Schema(description = "发送时刻 HH:mm")
    private String scheduleTime;

    @Schema(description = "截止时长(小时)")
    private Integer deadlineHours;

    @Schema(description = "上次发布时间")
    private LocalDateTime lastPublishTime;

    @Schema(description = "下次发布时间")
    private LocalDateTime nextPublishTime;

    @Schema(description = "已生成批次数")
    private Integer generatedCount;

    @Schema(description = "当前批次号")
    private Integer currentBatchNo;

    @Schema(description = "当前批次标签")
    private String currentBatchLabel;

    @Schema(description = "当前批次开始时间")
    private LocalDateTime currentBatchStartAt;

    @Schema(description = "当前批次截止时间")
    private LocalDateTime currentBatchEndAt;

    @Schema(description = "定时发送时间(一次性)")
    private LocalDateTime sendAt;

    @Schema(description = "截止时间")
    private LocalDateTime deadlineAt;

    @Schema(description = "提醒规则(JSON)")
    private String remindRuleJson;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "应填人数")
    private Integer totalCount;

    @Schema(description = "已交人数")
    private Integer submittedCount;

    @Schema(description = "未交人数")
    private Integer pendingCount;

    @Schema(description = "未交人员姓名")
    private List<String> pendingUserNames;

    @Schema(description = "公开问卷提交数")
    private Integer publicSubmittedCount;

}
