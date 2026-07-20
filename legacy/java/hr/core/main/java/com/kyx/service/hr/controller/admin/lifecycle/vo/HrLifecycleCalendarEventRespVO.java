package com.kyx.service.hr.controller.admin.lifecycle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - HR 生命周期日历事件 Response VO")
@Data
public class HrLifecycleCalendarEventRespVO {

    @Schema(description = "日历事件类型")
    private String eventType;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "日期")
    private LocalDate eventDate;

    @Schema(description = "员工档案 ID")
    private Long profileId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源 ID")
    private Long sourceId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "颜色")
    private String color;

}
