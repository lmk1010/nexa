package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 问卷发布保存 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷发布保存 VO")
@Data
public class QuestionnairePublishSaveReqVO {

    @Schema(description = "发布ID")
    private Long id;

    @Schema(description = "问卷ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "问卷ID不能为空")
    private Long questionnaireId;

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

    @Schema(description = "定时发送时间(一次性)")
    private LocalDateTime sendAt;

    @Schema(description = "截止时间")
    private LocalDateTime deadlineAt;

    @Schema(description = "提醒规则(JSON)")
    private String remindRuleJson;

    @Schema(description = "状态(0未发布 1已发布 2已截止)")
    private Integer status;

    @Schema(description = "撤回时是否清理已提交答卷")
    private Boolean clearSubmittedOnRevoke;

    @Schema(description = "编辑生效方式(0下一批生效 1立即生效)")
    private Integer applyMode;

}
