package com.kyx.service.biz.controller.admin.executive.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Schema(description = "Admin - Executive cockpit overview Response VO")
@Data
public class ExecutiveCockpitOverviewRespVO {

    private Date generatedAt;
    private Integer rangeDays;
    private Boolean ordersysAvailable;
    private String ordersysMessage;
    private List<MetricCard> metrics;
    private List<TrendPoint> requirementTrend;
    private List<StatusCount> requirementStatus;
    private List<AssigneeWorkload> workload;
    private List<RiskItem> risks;
    private List<RecentTask> recentOrdersysTasks;

    @Data
    public static class MetricCard {

        private String code;
        private String title;
        private String value;
        private String unit;
        private Long numericValue;
        private String description;
        private String trendLabel;
        private String tone;

    }

    @Data
    public static class TrendPoint {

        private String date;
        private Long createdCount;
        private Long finishedCount;

    }

    @Data
    public static class StatusCount {

        private Integer status;
        private String label;
        private Long count;
        private String tone;

    }

    @Data
    public static class AssigneeWorkload {

        private Long assigneeUserId;
        private String assigneeName;
        private Long totalCount;
        private Long openCount;
        private Long overdueCount;

    }

    @Data
    public static class RiskItem {

        private String source;
        private Long id;
        private String title;
        private Integer status;
        private String statusLabel;
        private Integer priority;
        private String priorityLabel;
        private String assigneeName;
        private Date expectedFinishDate;
        private Date updateTime;
        private String riskReason;

    }

    @Data
    public static class RecentTask {

        private String source;
        private Long id;
        private String title;
        private String status;
        private String statusLabel;
        private String operatorName;
        private String assigneeName;
        private Date taskTime;
        private Date updateTime;

    }

}
