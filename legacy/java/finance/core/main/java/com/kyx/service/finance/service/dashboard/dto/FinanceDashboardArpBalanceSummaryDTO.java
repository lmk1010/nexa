package com.kyx.service.finance.service.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 经营看板 - 应收应付余额汇总 DTO
 */
@Data
public class FinanceDashboardArpBalanceSummaryDTO {

    private BigDecimal receivableBalance;

    private BigDecimal payableBalance;
}
