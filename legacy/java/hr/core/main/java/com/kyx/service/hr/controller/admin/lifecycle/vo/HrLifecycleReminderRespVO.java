package com.kyx.service.hr.controller.admin.lifecycle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - HR 生命周期提醒 Response VO")
@Data
public class HrLifecycleReminderRespVO {

    @Schema(description = "提醒类型")
    private String reminderType;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "严重程度：HIGH/MEDIUM/LOW")
    private String severity;

    @Schema(description = "员工档案 ID")
    private Long profileId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "任职记录 ID")
    private Long entryId;

    @Schema(description = "生命周期事件 ID")
    private Long eventId;

    @Schema(description = "任务 ID")
    private Long taskId;

    @Schema(description = "到期日期")
    private LocalDate dueDate;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "建议动作")
    private String action;

}
