package com.kyx.service.finance.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 经营看板管理报表应收应付模块 Response VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 经营看板管理报表应收应付模块 Response VO")
public class FinanceDashboardReportArpRespVO {

    @Schema(description = "当前账期 yyyyMM", example = "202603")
    private String period;

    @Schema(description = "上一个账期 yyyyMM", example = "202602")
    private String previousPeriod;

    @Schema(description = "应收余额", example = "52000.00")
    private BigDecimal receivableBalanceAmount;

    @Schema(description = "上期应收余额", example = "50000.00")
    private BigDecimal previousReceivableBalanceAmount;

    @Schema(description = "应收余额较上期变动率（%）", example = "4.00")
    private BigDecimal receivableBalanceChangeRate;

    @Schema(description = "应付余额", example = "23000.00")
    private BigDecimal payableBalanceAmount;

    @Schema(description = "上期应付余额", example = "21000.00")
    private BigDecimal previousPayableBalanceAmount;

    @Schema(description = "应付余额较上期变动率（%）", example = "9.52")
    private BigDecimal payableBalanceChangeRate;

    @Schema(description = "预收余额", example = "12000.00")
    private BigDecimal advanceReceiptAmount;

    @Schema(description = "上期预收余额", example = "10000.00")
    private BigDecimal previousAdvanceReceiptAmount;

    @Schema(description = "预收余额较上期变动率（%）", example = "20.00")
    private BigDecimal advanceReceiptChangeRate;

    @Schema(description = "应收来往单位明细（最多3条）")
    private List<FinanceDashboardReportArpContactLineRespVO> receivableContactLines;

    @Schema(description = "应付来往单位明细（最多3条）")
    private List<FinanceDashboardReportArpContactLineRespVO> payableContactLines;
}
