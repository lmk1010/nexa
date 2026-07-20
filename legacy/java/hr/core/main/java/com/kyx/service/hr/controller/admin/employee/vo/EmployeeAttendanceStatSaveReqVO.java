package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 员工考勤统计新增/修改 Request VO")
@Data
public class EmployeeAttendanceStatSaveReqVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工档案ID不能为空")
    private Long profileId;

    @Schema(description = "统计年份", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "统计年份不能为空")
    private Integer statYear;

    @Schema(description = "统计月份", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "统计月份不能为空")
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
}
