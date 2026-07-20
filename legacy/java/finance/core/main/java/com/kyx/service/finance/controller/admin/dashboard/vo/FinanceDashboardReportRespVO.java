package com.kyx.service.finance.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 经营看板管理报表 Response VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 经营看板管理报表 Response VO")
public class FinanceDashboardReportRespVO {

    @Schema(description = "当前账期 yyyyMM", example = "202603")
    private String period;

    @Schema(description = "上一个账期 yyyyMM", example = "202602")
    private String previousPeriod;

    @Schema(description = "四周现金流（以账期锚点向前滚动四周，按成功流水收入-支出）", example = "30000.00")
    private BigDecimal fourWeekCashFlowAmount;

    @Schema(description = "上期四周现金流（按上期账期锚点向前滚动四周）", example = "28000.00")
    private BigDecimal previousFourWeekCashFlowAmount;

    @Schema(description = "账户余额", example = "260000.00")
    private BigDecimal accountBalanceAmount;

    @Schema(description = "上期账户余额", example = "250000.00")
    private BigDecimal previousAccountBalanceAmount;

    @Schema(description = "账户余额较上期变动率（%）", example = "4.00")
    private BigDecimal accountBalanceChangeRate;

    @Schema(description = "应收余额", example = "52000.00")
    private BigDecimal receivableBalanceAmount;

    @Schema(description = "上期应收余额", example = "50000.00")
    private BigDecimal previousReceivableBalanceAmount;

    @Schema(description = "应付余额", example = "23000.00")
    private BigDecimal payableBalanceAmount;

    @Schema(description = "上期应付余额", example = "21000.00")
    private BigDecimal previousPayableBalanceAmount;

    @Schema(description = "渠道余额分项（微信/支付宝/银行）")
    private List<FinanceDashboardReportChannelBalanceRespVO> channelBalanceLines;
}
