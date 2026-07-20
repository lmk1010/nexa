package com.kyx.service.finance.service.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 经营看板 - 收支汇总 DTO
 */
@Data
public class FinanceDashboardTransactionSummaryDTO {

    private BigDecimal incomeAmount;

    private BigDecimal expenseAmount;
}
