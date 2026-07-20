package com.kyx.service.hr.controller.admin.analytics.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Admin - HR analytics workbench Response VO")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class HrAnalyticsWorkbenchRespVO {

    private Summary summary = new Summary();

    private Trend trend = new Trend();

    private List<ExecutiveHighlight> executiveHighlights = new ArrayList<>();

    private List<MetricCard> metricCards = new ArrayList<>();

    private List<ChartItem> workforceStructure = new ArrayList<>();

    private List<ChartItem> recruitmentPipeline = new ArrayList<>();

    private List<ChartItem> recruitmentEfficiency = new ArrayList<>();

    private List<ChartItem> recruitmentChannelDistribution = new ArrayList<>();

    private List<ChartItem> recruitmentChannelEfficiency = new ArrayList<>();

    private List<ChartItem> recruitmentDemandHealth = new ArrayList<>();

    private List<ChartItem> performanceDistribution = new ArrayList<>();

    private List<ChartItem> trainingProgress = new ArrayList<>();

    private List<ChartItem> trainingQuality = new ArrayList<>();

    private List<ChartItem> lifecycleEventTypes = new ArrayList<>();

    private List<ChartItem> lifecycleEventStatus = new ArrayList<>();

    private List<ChartItem> lifecycleTaskStatus = new ArrayList<>();

    private List<ChartItem> todoHealth = new ArrayList<>();

    private List<ChartItem> employeeServiceBacklog = new ArrayList<>();

    private List<ChartItem> payrollCostTrend = new ArrayList<>();

    private List<ChartItem> payrollPerCapitaTrend = new ArrayList<>();

    private List<ChartItem> attendanceExceptionStatus = new ArrayList<>();

    private List<ChartItem> riskCategories = new ArrayList<>();

    private List<DeptOperationalItem> deptRiskTodoDistribution = new ArrayList<>();

    private List<Insight> insights = new ArrayList<>();

    private List<QuickAction> quickActions = new ArrayList<>();

    @Data
    public static class Summary {

        private Integer totalEmployees = 0;

        private Integer activeEmployees = 0;

        private Integer probationEmployees = 0;

        private Integer masterOrAboveEmployees = 0;

        private Integer masterOrAbovePercent = 0;

        private Integer educationFilledEmployees = 0;

        private Integer onboardingEmployees = 0;

        private Integer leavingEmployees = 0;

        private Integer newThisMonth = 0;

        private Integer leaveThisMonth = 0;

        private Integer stabilityIndex = 0;

        private BigDecimal avgAge = BigDecimal.ZERO;

        private Integer recruitmentTotal = 0;

        private Integer recruitmentOffer = 0;

        private Integer recruitmentEntry = 0;

        private Integer recruitmentOverdue = 0;

        private Integer recruitmentDemandApproved = 0;

        private Integer recruitmentInterviewEvaluated = 0;

        private Integer recruitmentReferral = 0;

        private Integer recruitmentTouched = 0;

        private BigDecimal recruitmentChannelCost = BigDecimal.ZERO;

        private BigDecimal recruitmentAvgInterviewScore = BigDecimal.ZERO;

        private Integer performanceTotal = 0;

        private BigDecimal avgPerformanceScore = BigDecimal.ZERO;

        private Integer performanceExcellent = 0;

        private Integer performanceWarning = 0;

        private Integer openTodoCount = 0;

        private Integer overdueTodoCount = 0;

        private Integer highPriorityTodoCount = 0;

        private Integer highRiskCount = 0;

        private Integer mediumRiskCount = 0;

        private Integer dataQualityScore = 0;

        private Integer pendingLifecycleEvents = 0;

        private Integer pendingAttendanceExceptions = 0;

        private Integer draftPayrollBatches = 0;

        private Integer payrollIssueCount = 0;

        private Integer trainingTotal = 0;

        private Integer trainingCompleted = 0;

        private Integer trainingOverdue = 0;

        private BigDecimal trainingSatisfactionRate = BigDecimal.ZERO;

        private Integer materialPendingReview = 0;

        private Integer materialExpiring = 0;

        private Integer documentPending = 0;

        private Integer reminderUnread = 0;

        private Integer socialSecurityAccounts = 0;
    }

    @Data
    public static class Trend {

        private List<String> months = new ArrayList<>();

        private List<Integer> totalTrend = new ArrayList<>();

        private List<Integer> activeTrend = new ArrayList<>();

        private List<Integer> onboardingTrend = new ArrayList<>();

        private List<Integer> probationTrend = new ArrayList<>();

        private List<Integer> leavingTrend = new ArrayList<>();

        private List<Integer> stabilityTrend = new ArrayList<>();
    }

    @Data
    public static class MetricCard {

        private String code;

        private String title;

        private String value;

        private String unit;

        private String subtitle;

        private String icon;

        private String tone;

        private String path;
    }

    @Data
    public static class ExecutiveHighlight {

        private String code;

        private String title;

        private String value;

        private String subtitle;

        private String icon;

        private String tone;

        private String path;
    }

    @Data
    public static class ChartItem {

        private String code;

        private String name;

        private Integer value = 0;

        private Integer percent = 0;

        private String tone;

        private String path;
    }

    @Data
    public static class Insight {

        private String severity;

        private String title;

        private String description;

        private String action;

        private String path;

        private String icon;
    }

    @Data
    public static class DeptOperationalItem {

        private Long deptId;

        private String deptName;

        private Integer headcount = 0;

        private Integer openTodoCount = 0;

        private Integer overdueTodoCount = 0;

        private Integer highRiskCount = 0;

        private Integer mediumRiskCount = 0;

        private Integer openRiskCount = 0;

        private Integer score = 0;

        private String tone;

        private String path;
    }

    @Data
    public static class QuickAction {

        private String title;

        private String icon;

        private String path;

        private String description;
    }
}
