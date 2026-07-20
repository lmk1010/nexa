package com.kyx.service.finance.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 现金流量表 Response VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 现金流量表 Response VO")
public class FinanceCashflowStatementRespVO {

    @Schema(description = "本期（yyyyMM）", example = "202603")
    private String period;

    @Schema(description = "上期（yyyyMM）", example = "202602")
    private String previousPeriod;

    @Schema(description = "期末现金余额", example = "180000.00")
    private BigDecimal endingCashAmount;

    @Schema(description = "净现金增加额", example = "12000.00")
    private BigDecimal netCashIncreaseAmount;

    @Schema(description = "经营活动现金流", example = "8000.00")
    private BigDecimal operatingCashFlowAmount;

    @Schema(description = "投资活动现金流", example = "-2000.00")
    private BigDecimal investingCashFlowAmount;

    @Schema(description = "筹资活动现金流", example = "6000.00")
    private BigDecimal financingCashFlowAmount;

    @Schema(description = "对比行明细")
    private List<FinanceAmountComparisonItemRespVO> rows;
}
