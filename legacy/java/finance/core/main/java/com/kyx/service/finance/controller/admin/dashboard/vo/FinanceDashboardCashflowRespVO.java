package com.kyx.service.finance.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 管理后台 - 经营看板现金流量统计 Response VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 经营看板现金流量统计 Response VO")
public class FinanceDashboardCashflowRespVO {

    @Schema(description = "当前账期 yyyyMM", example = "202603")
    private String period;

    @Schema(description = "上一个账期 yyyyMM", example = "202602")
    private String previousPeriod;

    @Schema(description = "期末现金金额", example = "260000.00")
    private BigDecimal endingCashAmount;

    @Schema(description = "上期期末现金金额", example = "250000.00")
    private BigDecimal previousEndingCashAmount;

    @Schema(description = "现金净增加额", example = "30000.00")
    private BigDecimal netCashIncreaseAmount;

    @Schema(description = "上期现金净增加额", example = "28000.00")
    private BigDecimal previousNetCashIncreaseAmount;

    @Schema(description = "经营活动现金流", example = "26000.00")
    private BigDecimal operatingCashFlowAmount;

    @Schema(description = "投资活动现金流", example = "-2000.00")
    private BigDecimal investingCashFlowAmount;

    @Schema(description = "筹资活动现金流", example = "6000.00")
    private BigDecimal financingCashFlowAmount;
}

