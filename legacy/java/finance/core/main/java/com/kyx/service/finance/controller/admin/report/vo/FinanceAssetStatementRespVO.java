package com.kyx.service.finance.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 资产负债表 Response VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 资产负债表 Response VO")
public class FinanceAssetStatementRespVO {

    @Schema(description = "本期（yyyyMM）", example = "202603")
    private String period;

    @Schema(description = "上期（yyyyMM）", example = "202602")
    private String previousPeriod;

    @Schema(description = "账户余额", example = "200000.00")
    private BigDecimal accountBalanceAmount;

    @Schema(description = "应收余额", example = "50000.00")
    private BigDecimal receivableBalanceAmount;

    @Schema(description = "应付余额", example = "30000.00")
    private BigDecimal payableBalanceAmount;

    @Schema(description = "资产合计", example = "250000.00")
    private BigDecimal assetAmount;

    @Schema(description = "负债合计", example = "30000.00")
    private BigDecimal liabilityAmount;

    @Schema(description = "所有者权益", example = "220000.00")
    private BigDecimal equityAmount;

    @Schema(description = "对比行明细")
    private List<FinanceAmountComparisonItemRespVO> rows;
}
