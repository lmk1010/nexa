package com.kyx.service.biz.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "Admin - Work Requirement overview Response VO")
@Data
public class WorkRequirementOverviewRespVO {

    private Long totalCount;
    private Long pendingCount;
    private Long developingCount;
    private Long completedCount;
    private Long myTodoCount;
    private Long overdueCount;
    private Long unreadCount;
    private List<StatusCount> statusCounts;
    private List<DailyTrend> trendDays;

    @Data
    public static class StatusCount {

        private Integer status;
        private Long count;

    }

    @Data
    public static class DailyTrend {

        private String date;
        private Long createdCount;
        private Long finishedCount;

    }

}
