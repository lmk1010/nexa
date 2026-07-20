package com.kyx.service.finance.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 经营看板聚合数据 Response VO")
public class FinanceDashboardAggregateRespVO {

    @Schema(description = "经营概览模块")
    private FinanceDashboardOperatingRespVO operating;

    @Schema(description = "管理报表模块")
    private FinanceDashboardReportRespVO report;

    @Schema(description = "管理报表应收应付模块")
    private FinanceDashboardReportArpRespVO reportArp;

    @Schema(description = "资产负债模块")
    private FinanceDashboardAssetRespVO asset;

    @Schema(description = "现金流量统计模块")
    private FinanceDashboardCashflowRespVO cashflow;
}
