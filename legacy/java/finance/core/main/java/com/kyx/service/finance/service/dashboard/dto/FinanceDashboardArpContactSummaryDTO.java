package com.kyx.service.finance.service.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 经营看板 - 应收应付往来单位汇总 DTO
 *
 * @author xyang
 */
@Data
public class FinanceDashboardArpContactSummaryDTO {

    private Long contactId;

    private String contactName;

    private BigDecimal amount;
}

