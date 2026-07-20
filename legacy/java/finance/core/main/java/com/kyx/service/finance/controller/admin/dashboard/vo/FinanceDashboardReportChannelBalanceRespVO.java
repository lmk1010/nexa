package com.kyx.service.finance.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 管理后台 - 经营看板管理报表渠道余额分项 Response VO
 * @author xyang
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 经营看板管理报表渠道余额分项 Response VO")
public class FinanceDashboardReportChannelBalanceRespVO {

    @Schema(description = "渠道类型", example = "WECHAT")
    private String channelType;

    @Schema(description = "渠道名称", example = "微信")
    private String channelName;

    @Schema(description = "当前账期余额", example = "13000.00")
    private BigDecimal amount;

    @Schema(description = "上期余额", example = "12600.00")
    private BigDecimal previousAmount;

    @Schema(description = "较上期变动率（%）", example = "3.17")
    private BigDecimal changeRate;
}

