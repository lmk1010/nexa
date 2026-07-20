package com.kyx.service.finance.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 经营看板总览 Response VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 经营看板总览 Response VO")
public class FinanceDashboardOverviewRespVO {

    @Schema(description = "统计期间 yyyyMM", example = "202603")
    private String period;

    @Schema(description = "经营收入", example = "120000.00")
    private BigDecimal operatingIncomeAmount;

    @Schema(description = "经营支出", example = "90000.00")
    private BigDecimal operatingExpenseAmount;

    @Schema(description = "经营利润", example = "30000.00")
    private BigDecimal operatingProfitAmount;

    @Schema(description = "账户余额", example = "260000.00")
    private BigDecimal accountBalanceAmount;

    @Schema(description = "应收余额", example = "52000.00")
    private BigDecimal receivableBalanceAmount;

    @Schema(description = "应付余额", example = "23000.00")
    private BigDecimal payableBalanceAmount;

    @Schema(description = "四周现金流（近四周）")
    private List<FinanceDashboardWeeklyCashFlowRespVO> weeklyCashFlowList;

    @Schema(description = "资产", example = "312000.00")
    private BigDecimal assetAmount;

    @Schema(description = "负债", example = "23000.00")
    private BigDecimal liabilityAmount;

    @Schema(description = "所有者权益", example = "289000.00")
    private BigDecimal equityAmount;

    @Schema(description = "期末现金金额", example = "260000.00")
    private BigDecimal endingCashAmount;

    @Schema(description = "现金净增加额", example = "30000.00")
    private BigDecimal netCashIncreaseAmount;

    @Schema(description = "经营活动现金流", example = "26000.00")
    private BigDecimal operatingCashFlowAmount;

    @Schema(description = "投资活动现金流", example = "-2000.00")
    private BigDecimal investingCashFlowAmount;

    @Schema(description = "筹资活动现金流", example = "6000.00")
    private BigDecimal financingCashFlowAmount;
}
