package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 员工培训统计 Response VO")
@Data
public class EmployeeTrainingStatsRespVO {

    private Integer totalCount = 0;

    private Integer completedCount = 0;

    private Integer inProgressCount = 0;

    private Integer upcomingCount = 0;

    private Integer overdueCount = 0;

    private Integer retrainDueCount = 0;

    private Integer certificateExpiringCount = 0;

    private Integer certificateLinkedCount = 0;

    private Integer evaluatedCount = 0;

    private BigDecimal totalHours = BigDecimal.ZERO;

    private BigDecimal averageHoursPerRecord = BigDecimal.ZERO;

    private BigDecimal completionRate = BigDecimal.ZERO;

    private BigDecimal overdueRate = BigDecimal.ZERO;

    private BigDecimal certificateCoverageRate = BigDecimal.ZERO;

    private BigDecimal averageEvaluationScore = BigDecimal.ZERO;

    private BigDecimal satisfactionRate = BigDecimal.ZERO;

    private Integer examLinkedCount = 0;

    private Integer examSubmittedCount = 0;

    private Integer examPassedCount = 0;

    private BigDecimal examAverageScore = BigDecimal.ZERO;

    private BigDecimal examPassRate = BigDecimal.ZERO;

    private List<StatItem> providerStats = new ArrayList<>();

    private List<StatItem> resultStats = new ArrayList<>();

    @Data
    public static class StatItem {

        private String name;

        private Integer count;

        public StatItem(String name, Integer count) {
            this.name = name;
            this.count = count;
        }
    }
}
