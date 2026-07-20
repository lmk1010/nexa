package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Admin - Employee recruitment statistics Response VO")
@Data
public class EmployeeRecruitmentStatsRespVO {

    private Integer totalCount = 0;

    private Integer offerCount = 0;

    private Integer interviewCount = 0;

    private Integer offerAcceptedCount = 0;

    private Integer pendingEntryCount = 0;

    private Integer entryCount = 0;

    private Integer overdueFollowCount = 0;

    private Integer highPriorityCount = 0;

    private Integer poolCount = 0;

    private Integer blacklistCount = 0;

    private Integer resumeCount = 0;

    private Integer resumeParsedCount = 0;

    private Integer demandOpenCount = 0;

    private Integer demandApprovedCount = 0;

    private Integer interviewEvaluatedCount = 0;

    private Integer interviewPassCount = 0;

    private BigDecimal avgInterviewScore = BigDecimal.ZERO;

    private Integer referralCount = 0;

    private Integer touchedCount = 0;

    private Integer responseCount = 0;

    private BigDecimal channelCostTotal = BigDecimal.ZERO;

    private BigDecimal responseRate = BigDecimal.ZERO;

    private BigDecimal interviewRate = BigDecimal.ZERO;

    private BigDecimal offerRate = BigDecimal.ZERO;

    private BigDecimal entryRate = BigDecimal.ZERO;

    private BigDecimal costPerCandidate = BigDecimal.ZERO;

    private BigDecimal costPerEntry = BigDecimal.ZERO;

    private List<StatItem> channelStats = new ArrayList<>();

    private List<ChannelEffectItem> channelEffectStats = new ArrayList<>();

    private List<StatItem> statusStats = new ArrayList<>();

    private List<StatItem> stageStats = new ArrayList<>();

    private List<StatItem> talentStatusStats = new ArrayList<>();

    private List<StatItem> demandStatusStats = new ArrayList<>();

    private List<StatItem> interviewDecisionStats = new ArrayList<>();

    private List<StatItem> campaignStats = new ArrayList<>();

    private List<StatItem> touchStatusStats = new ArrayList<>();

    private List<StatItem> resumeParseStatusStats = new ArrayList<>();

    @Data
    public static class StatItem {

        private String name;

        private Integer count;

        public StatItem(String name, Integer count) {
            this.name = name;
            this.count = count;
        }
    }

    @Data
    public static class ChannelEffectItem {

        private String name;

        private Integer candidateCount = 0;

        private Integer touchedCount = 0;

        private Integer responseCount = 0;

        private Integer interviewCount = 0;

        private Integer offerCount = 0;

        private Integer entryCount = 0;

        private BigDecimal channelCost = BigDecimal.ZERO;

        private BigDecimal costPerCandidate = BigDecimal.ZERO;

        private BigDecimal costPerEntry = BigDecimal.ZERO;

        private BigDecimal responseRate = BigDecimal.ZERO;

        private BigDecimal interviewRate = BigDecimal.ZERO;

        private BigDecimal offerRate = BigDecimal.ZERO;

        private BigDecimal entryRate = BigDecimal.ZERO;

        public ChannelEffectItem(String name) {
            this.name = name;
        }
    }
}
