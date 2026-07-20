package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工考勤统计 Response VO")
@Data
public class EmployeeAttendanceStatRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long profileId;

    @Schema(description = "统计年份")
    private Integer statYear;

    @Schema(description = "统计月份")
    private Integer statMonth;

    @Schema(description = "应出勤天数")
    private Integer workDays;

    @Schema(description = "实际出勤天数")
    private Integer actualDays;

    @Schema(description = "迟到次数")
    private Integer lateCount;

    @Schema(description = "早退次数")
    private Integer earlyLeaveCount;

    @Schema(description = "缺勤次数")
    private Integer absentCount;

    @Schema(description = "缺卡次数")
    private Integer missingClockCount;

    @Schema(description = "加班时长(小时)")
    private BigDecimal overtimeHours;

    @Schema(description = "请假天数")
    private BigDecimal leaveDays;

    @Schema(description = "出差天数")
    private BigDecimal tripDays;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
