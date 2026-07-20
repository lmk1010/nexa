package com.kyx.service.finance.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 管理后台 - 金额对比行数据 Response VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 金额对比行数据 Response VO")
public class FinanceAmountComparisonItemRespVO {

    @Schema(description = "项目编码", example = "ASSET")
    private String itemCode;

    @Schema(description = "项目名称", example = "资产")
    private String itemName;

    @Schema(description = "本期金额", example = "12345.67")
    private BigDecimal currentAmount;

    @Schema(description = "上期金额", example = "12000.00")
    private BigDecimal previousAmount;

    @Schema(description = "环比（%）", example = "2.88")
    private BigDecimal changeRate;
}
