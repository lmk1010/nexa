package com.kyx.service.finance.service.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 利润表聚合结果 DTO
 */
@Data
public class FinanceIncomeStatementAggregateDTO {

    /**
     * 科目编码
     */
    private String subjectCode;

    /**
     * 收入金额
     */
    private BigDecimal incomeAmount;

    /**
     * 支出金额
     */
    private BigDecimal expenseAmount;
}
