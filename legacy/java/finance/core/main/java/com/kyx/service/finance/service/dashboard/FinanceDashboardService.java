package com.kyx.service.finance.service.dashboard;

import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardMetricsReqVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardMetricsRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardOperatingRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardReportRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardReportArpRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardAssetRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardCashflowRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardOverviewRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardAggregateRespVO;

/**
 * 经营看板 Service
 */
public interface FinanceDashboardService {

    /**
     * 查询经营看板基础指标
     *
     * @param reqVO 查询条件
     * @return 指标数据
     */
    FinanceDashboardMetricsRespVO getMetrics(FinanceDashboardMetricsReqVO reqVO);

    /**
     * 查询经营概览模块
     *
     * @param reqVO 查询条件
     * @return 经营概览
     */
    FinanceDashboardOperatingRespVO getOperatingModule(FinanceDashboardMetricsReqVO reqVO);

    /**
     * 查询管理报表模块
     *
     * @param reqVO 查询条件
     * @return 管理报表
     */
    FinanceDashboardReportRespVO getReportModule(FinanceDashboardMetricsReqVO reqVO);

    /**
     * 查询管理报表-应收应付（按月）模块
     *
     * @param reqVO 查询条件
     * @return 应收应付模块
     */
    FinanceDashboardReportArpRespVO getReportArpModule(FinanceDashboardMetricsReqVO reqVO);

    /**
     * 查询资产负债模块
     *
     * @param reqVO 查询条件
     * @return 资产负债
     */
    FinanceDashboardAssetRespVO getAssetModule(FinanceDashboardMetricsReqVO reqVO);

    /**
     * 查询现金流量统计模块
     *
     * @param reqVO 查询条件
     * @return 现金流量统计
     */
    FinanceDashboardCashflowRespVO getCashflowModule(FinanceDashboardMetricsReqVO reqVO);

    /**
     * 查询经营看板总览（含图表）
     *
     * @param reqVO 查询条件
     * @return 看板总览
     */
    FinanceDashboardOverviewRespVO getOverview(FinanceDashboardMetricsReqVO reqVO);

    /**
     * 查询经营看板聚合数据（一次返回所有模块）
     *
     * @param reqVO 查询条件
     * @return 聚合数据
     */
    FinanceDashboardAggregateRespVO getAggregate(FinanceDashboardMetricsReqVO reqVO);
}
