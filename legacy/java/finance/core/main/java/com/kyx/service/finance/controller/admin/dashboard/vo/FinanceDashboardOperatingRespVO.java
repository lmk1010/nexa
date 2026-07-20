package com.kyx.service.finance.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 管理后台 - 经营看板经营概览 Response VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 经营看板经营概览 Response VO")
public class FinanceDashboardOperatingRespVO {

    @Schema(description = "当前账期 yyyyMM", example = "202603")
    private String period;

    @Schema(description = "上一个账期 yyyyMM", example = "202602")
    private String previousPeriod;

    @Schema(description = "经营收入", example = "120000.00")
    private BigDecimal operatingIncomeAmount;

    @Schema(description = "上期经营收入", example = "110000.00")
    private BigDecimal previousOperatingIncomeAmount;

    @Schema(description = "经营支出", example = "90000.00")
    private BigDecimal operatingExpenseAmount;

    @Schema(description = "上期经营支出", example = "82000.00")
    private BigDecimal previousOperatingExpenseAmount;

    @Schema(description = "经营利润", example = "30000.00")
    private BigDecimal operatingProfitAmount;

    @Schema(description = "上期经营利润", example = "28000.00")
    private BigDecimal previousOperatingProfitAmount;

    @Schema(description = "经营收入较上期变动率（%）", example = "9.09")
    private BigDecimal operatingIncomeChangeRate;

    @Schema(description = "经营支出较上期变动率（%）", example = "9.76")
    private BigDecimal operatingExpenseChangeRate;

    @Schema(description = "经营利润较上期变动率（%）", example = "7.14")
    private BigDecimal operatingProfitChangeRate;
}
