package com.kyx.service.hr.service.analytics;

import com.kyx.service.hr.controller.admin.analytics.vo.HrAnalyticsWorkbenchRespVO;

public interface HrAnalyticsWorkbenchService {

    HrAnalyticsWorkbenchRespVO getWorkbench();

    HrAnalyticsWorkbenchRespVO getWorkbenchOverview();

    HrAnalyticsWorkbenchRespVO getWorkbenchSignals();

    HrAnalyticsWorkbenchRespVO getWorkbenchCoreCharts();

    HrAnalyticsWorkbenchRespVO getWorkbenchExtendedCharts();

}
