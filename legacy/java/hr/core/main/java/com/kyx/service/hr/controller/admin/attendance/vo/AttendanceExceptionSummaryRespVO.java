package com.kyx.service.hr.controller.admin.attendance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AttendanceExceptionSummaryRespVO {

    private Integer totalCount = 0;

    private Integer pendingCount = 0;

    private Integer resolvedCount = 0;

    private Integer ignoredCount = 0;

    private BigDecimal pendingRate = BigDecimal.ZERO;

    private List<StatItem> typeStats = new ArrayList<>();

    private List<StatItem> statusStats = new ArrayList<>();

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
