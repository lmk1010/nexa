package com.kyx.service.finance.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 管理后台 - 经营看板周现金流 Response VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 经营看板周现金流 Response VO")
public class FinanceDashboardWeeklyCashFlowRespVO {

    @Schema(description = "周起始日期 yyyy-MM-dd", example = "2026-03-02")
    private String weekStartDate;

    @Schema(description = "收入", example = "30000.00")
    private BigDecimal incomeAmount;

    @Schema(description = "支出", example = "22000.00")
    private BigDecimal expenseAmount;

    @Schema(description = "净现金流（收入-支出）", example = "8000.00")
    private BigDecimal netCashFlowAmount;
}
