package com.kyx.service.finance.controller.admin.dashboard;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardMetricsReqVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardMetricsRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardOperatingRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardReportRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardReportArpRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardAssetRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardCashflowRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardOverviewRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardAggregateRespVO;
import com.kyx.service.finance.service.dashboard.FinanceDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 经营看板 Controller
 */
@RestController
@RequestMapping("/finance/dashboard")
@Tag(name = "财务管理 - 经营看板")
@Validated
public class FinanceDashboardController {

    @Resource
    private FinanceDashboardService financeDashboardService;

    @Operation(summary = "查询经营看板基础指标")
    @GetMapping("/metrics")
    @PreAuthorize("@ss.hasAnyPermissions('finance:dashboard:query,finance:dashboard:list')")
    public CommonResult<FinanceDashboardMetricsRespVO> getMetrics(@Valid FinanceDashboardMetricsReqVO reqVO) {
        return success(financeDashboardService.getMetrics(reqVO));
    }

    @Operation(summary = "查询经营概览模块")
    @GetMapping("/operating")
    @PreAuthorize("@ss.hasAnyPermissions('finance:dashboard:query,finance:dashboard:list')")
    public CommonResult<FinanceDashboardOperatingRespVO> getOperatingModule(@Valid FinanceDashboardMetricsReqVO reqVO) {
        return success(financeDashboardService.getOperatingModule(reqVO));
    }

    @Operation(summary = "查询管理报表模块")
    @GetMapping("/report")
    @PreAuthorize("@ss.hasAnyPermissions('finance:dashboard:query,finance:dashboard:list')")
    public CommonResult<FinanceDashboardReportRespVO> getReportModule(@Valid FinanceDashboardMetricsReqVO reqVO) {
        return success(financeDashboardService.getReportModule(reqVO));
    }

    @Operation(summary = "查询管理报表模块-应收应付（按账期）")
    @GetMapping("/report/arp")
    @PreAuthorize("@ss.hasAnyPermissions('finance:dashboard:query,finance:dashboard:list')")
    public CommonResult<FinanceDashboardReportArpRespVO> getReportArpModule(@Valid FinanceDashboardMetricsReqVO reqVO) {
        return success(financeDashboardService.getReportArpModule(reqVO));
    }

    @Operation(summary = "查询资产负债模块")
    @GetMapping("/asset")
    @PreAuthorize("@ss.hasAnyPermissions('finance:dashboard:query,finance:dashboard:list')")
    public CommonResult<FinanceDashboardAssetRespVO> getAssetModule(@Valid FinanceDashboardMetricsReqVO reqVO) {
        return success(financeDashboardService.getAssetModule(reqVO));
    }

    @Operation(summary = "查询现金流量统计模块")
    @GetMapping("/cashflow")
    @PreAuthorize("@ss.hasAnyPermissions('finance:dashboard:query,finance:dashboard:list')")
    public CommonResult<FinanceDashboardCashflowRespVO> getCashflowModule(@Valid FinanceDashboardMetricsReqVO reqVO) {
        return success(financeDashboardService.getCashflowModule(reqVO));
    }

    @Operation(summary = "查询经营看板总览（含图表）")
    @GetMapping("/overview")
    @PreAuthorize("@ss.hasAnyPermissions('finance:dashboard:query,finance:dashboard:list')")
    public CommonResult<FinanceDashboardOverviewRespVO> getOverview(@Valid FinanceDashboardMetricsReqVO reqVO) {
        return success(financeDashboardService.getOverview(reqVO));
    }

    @Operation(summary = "查询经营看板聚合数据（一次返回所有模块）")
    @GetMapping("/aggregate")
    @PreAuthorize("@ss.hasAnyPermissions('finance:dashboard:query,finance:dashboard:list')")
    public CommonResult<FinanceDashboardAggregateRespVO> getAggregate(@Valid FinanceDashboardMetricsReqVO reqVO) {
        return success(financeDashboardService.getAggregate(reqVO));
    }
}
