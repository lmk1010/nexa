package com.kyx.service.finance.service.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 经营看板 - 渠道余额汇总 DTO
 */
@Data
public class FinanceDashboardChannelBalanceSummaryDTO {

    private String accountType;

    private BigDecimal balance;
}

