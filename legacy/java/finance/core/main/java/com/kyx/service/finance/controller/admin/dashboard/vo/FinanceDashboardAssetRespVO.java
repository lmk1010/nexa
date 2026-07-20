package com.kyx.service.finance.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 管理后台 - 经营看板资产负债 Response VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 经营看板资产负债 Response VO")
public class FinanceDashboardAssetRespVO {

    @Schema(description = "当前账期 yyyyMM", example = "202603")
    private String period;

    @Schema(description = "上一个账期 yyyyMM", example = "202602")
    private String previousPeriod;

    @Schema(description = "资产", example = "312000.00")
    private BigDecimal assetAmount;

    @Schema(description = "上期资产", example = "298000.00")
    private BigDecimal previousAssetAmount;

    @Schema(description = "负债和所有者权益", example = "23000.00")
    private BigDecimal liabilityAmount;

    @Schema(description = "上期负债和所有者权益", example = "21000.00")
    private BigDecimal previousLiabilityAmount;

    @Schema(description = "所有者权益", example = "289000.00")
    private BigDecimal equityAmount;

    @Schema(description = "上期所有者权益", example = "277000.00")
    private BigDecimal previousEquityAmount;
}

