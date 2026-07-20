package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Admin - Employee performance statistics Response VO")
@Data
public class EmployeePerformanceStatsRespVO {

    private Integer totalCount = 0;

    private BigDecimal avgScore = BigDecimal.ZERO;

    private Integer excellentCount = 0;

    private Integer warningCount = 0;

    private Integer recentCount = 0;

    private Integer pendingCount = 0;

    private Integer closedCount = 0;

    private Integer interviewScheduledCount = 0;

    private Integer followUpCount = 0;

    private Integer applicationPendingCount = 0;

    private Integer applicationAppliedCount = 0;

    private Integer approvalSubmittedCount = 0;

    private Integer approvalApprovedCount = 0;

    private Integer approvalRejectedCount = 0;

    private List<StatItem> statusStats = new ArrayList<>();

    private List<StatItem> gradeStats = new ArrayList<>();

    private List<StatItem> resultStats = new ArrayList<>();

    private List<StatItem> applicationStatusStats = new ArrayList<>();

    private List<StatItem> applicationTypeStats = new ArrayList<>();

    private List<StatItem> approvalStatusStats = new ArrayList<>();

    @Data
    public static class StatItem {

        private String code;

        private String name;

        private Integer count;

        public StatItem(String name, Integer count) {
            this.code = name;
            this.name = name;
            this.count = count;
        }

        public StatItem(String code, String name, Integer count) {
            this.code = code;
            this.name = name;
            this.count = count;
        }
    }
}
