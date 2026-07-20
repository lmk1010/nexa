package com.kyx.service.finance.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 管理后台 - 经营看板指标 Response VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 经营看板指标 Response VO")
public class FinanceDashboardMetricsRespVO {

    @Schema(description = "统计期间 yyyyMM", example = "202603")
    private String period;

    @Schema(description = "收入", example = "120000.00")
    private BigDecimal incomeAmount;

    @Schema(description = "支出", example = "80000.00")
    private BigDecimal expenseAmount;

    @Schema(description = "净额（收入-支出）", example = "40000.00")
    private BigDecimal netAmount;

    @Schema(description = "应收余额", example = "52000.00")
    private BigDecimal receivableBalance;

    @Schema(description = "应付余额", example = "23000.00")
    private BigDecimal payableBalance;
}
