package com.kyx.service.hr.controller.admin.lifecycle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 生命周期事件 Response VO")
@Data
public class HrLifecycleEventRespVO {

    @Schema(description = "事件 ID")
    private Long id;

    @Schema(description = "员工档案 ID")
    private Long profileId;

    @Schema(description = "任职记录 ID")
    private Long entryId;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "事件状态")
    private String eventStatus;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源 ID")
    private Long sourceId;

    @Schema(description = "流程实例 ID")
    private String processInstanceId;

    @Schema(description = "发起人 ID")
    private Long applyUserId;

    @Schema(description = "发起人姓名")
    private String applyUserName;

    @Schema(description = "发起时间")
    private LocalDateTime applyTime;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "完成时间")
    private LocalDateTime completedTime;

    @Schema(description = "变更前 JSON")
    private String beforeJson;

    @Schema(description = "变更后 JSON")
    private String afterJson;

    @Schema(description = "原因")
    private String reason;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "任务总数")
    private Integer totalTaskCount;

    @Schema(description = "已完成任务数")
    private Integer completedTaskCount;

    @Schema(description = "未完成必办任务数")
    private Integer openRequiredTaskCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
