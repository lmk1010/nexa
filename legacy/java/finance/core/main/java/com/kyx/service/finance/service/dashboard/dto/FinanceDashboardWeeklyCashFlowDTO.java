package com.kyx.service.finance.service.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 经营看板 - 周现金流汇总 DTO
 */
@Data
public class FinanceDashboardWeeklyCashFlowDTO {

    /**
     * 周起始日期 yyyy-MM-dd（周一）
     */
    private String weekStartDate;

    private BigDecimal incomeAmount;

    private BigDecimal expenseAmount;
}
