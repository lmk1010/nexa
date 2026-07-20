package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 考勤工作台 Response VO")
@Data
public class AttendanceWorkbenchRespVO {

    @Schema(description = "统计日期")
    private LocalDate today;

    @Schema(description = "在职员工数")
    private Integer activeEmployeeCount;

    @Schema(description = "今日已打卡人数")
    private Integer todayClockUserCount;

    @Schema(description = "今日上班打卡次数")
    private Integer todayClockInCount;

    @Schema(description = "今日下班打卡次数")
    private Integer todayClockOutCount;

    @Schema(description = "今日请假人数")
    private Integer todayLeaveUserCount;

    @Schema(description = "今日出差人数")
    private Integer todayTripUserCount;

    @Schema(description = "审批中请假数")
    private Integer runningLeaveCount;

    @Schema(description = "审批中出差数")
    private Integer runningTripCount;

    @Schema(description = "待处理补卡数")
    private Integer pendingCorrectionCount;

    @Schema(description = "待处理加班数")
    private Integer pendingOvertimeCount;

    @Schema(description = "待确认月度考勤数")
    private Integer pendingMonthlyConfirmCount;

    @Schema(description = "待处理月度异议数")
    private Integer pendingMonthlyIssueCount;

    @Schema(description = "待处理考勤异常数")
    private Integer pendingExceptionCount;

    @Schema(description = "待办总数")
    private Integer pendingTodoCount;

    @Schema(description = "本月考勤汇总")
    private AttendanceClockRecordSummaryRespVO monthSummary;

    @Schema(description = "提醒列表")
    private List<Alert> alerts;

    @Schema(description = "快捷入口")
    private List<QuickAction> quickActions;

    @Schema(description = "提醒")
    @Data
    public static class Alert {

        @Schema(description = "类型")
        private String type;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "数量")
        private Integer count;

        @Schema(description = "级别")
        private String severity;

        @Schema(description = "前端路径")
        private String path;
    }

    @Schema(description = "快捷入口")
    @Data
    public static class QuickAction {

        @Schema(description = "标题")
        private String title;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "前端路径")
        private String path;
    }

}
