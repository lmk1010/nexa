package com.kyx.service.finance.service.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 经营看板 - 现金流活动汇总 DTO
 */
@Data
public class FinanceDashboardCashFlowActivitySummaryDTO {

    private BigDecimal operatingNetCash;

    private BigDecimal investingNetCash;

    private BigDecimal financingNetCash;
}
