package com.kyx.service.hr.controller.admin.lifecycle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 生命周期任务 Response VO")
@Data
public class HrLifecycleTaskRespVO {

    @Schema(description = "任务 ID")
    private Long id;

    @Schema(description = "事件 ID")
    private Long eventId;

    @Schema(description = "员工档案 ID")
    private Long profileId;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "负责人用户 ID")
    private Long assigneeUserId;

    @Schema(description = "负责人姓名")
    private String assigneeName;

    @Schema(description = "截止日期")
    private LocalDate dueDate;

    @Schema(description = "任务状态")
    private String taskStatus;

    @Schema(description = "是否必办")
    private Boolean requiredFlag;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "完成时间")
    private LocalDateTime completedTime;

    @Schema(description = "完成人")
    private Long completedBy;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
