package com.kyx.service.hr.controller.admin.analytics;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.analytics.vo.HrAnalyticsWorkbenchRespVO;
import com.kyx.service.hr.service.analytics.HrAnalyticsWorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "Admin - HR analytics")
@RestController
@RequestMapping("/hr/analytics")
@Validated
public class HrAnalyticsController {

    private static final String HR_ANALYTICS_ACCESS_CHECK =
            "@ss.hasAnyRoles('super_admin', 'tenant_admin', 'system_admin', 'admin', 'administrator', " +
                    "'HROwner', 'hrowner', 'hr_admin', 'hr_manager', 'hr_owner', 'human_resources', 'biz_boss')";

    @Resource
    private HrAnalyticsWorkbenchService hrAnalyticsWorkbenchService;

    @GetMapping("/workbench")
    @Operation(summary = "Get HR analytics workbench")
    @PreAuthorize(HR_ANALYTICS_ACCESS_CHECK)
    public CommonResult<HrAnalyticsWorkbenchRespVO> getWorkbench() {
        return success(hrAnalyticsWorkbenchService.getWorkbench());
    }

    @GetMapping("/workbench/overview")
    @Operation(summary = "Get HR analytics overview")
    @PreAuthorize(HR_ANALYTICS_ACCESS_CHECK)
    public CommonResult<HrAnalyticsWorkbenchRespVO> getWorkbenchOverview() {
        return success(hrAnalyticsWorkbenchService.getWorkbenchOverview());
    }

    @GetMapping("/workbench/signals")
    @Operation(summary = "Get HR analytics operational signals")
    @PreAuthorize(HR_ANALYTICS_ACCESS_CHECK)
    public CommonResult<HrAnalyticsWorkbenchRespVO> getWorkbenchSignals() {
        return success(hrAnalyticsWorkbenchService.getWorkbenchSignals());
    }

    @GetMapping("/workbench/charts/core")
    @Operation(summary = "Get HR analytics core charts")
    @PreAuthorize(HR_ANALYTICS_ACCESS_CHECK)
    public CommonResult<HrAnalyticsWorkbenchRespVO> getWorkbenchCoreCharts() {
        return success(hrAnalyticsWorkbenchService.getWorkbenchCoreCharts());
    }

    @GetMapping("/workbench/charts/extended")
    @Operation(summary = "Get HR analytics extended charts")
    @PreAuthorize(HR_ANALYTICS_ACCESS_CHECK)
    public CommonResult<HrAnalyticsWorkbenchRespVO> getWorkbenchExtendedCharts() {
        return success(hrAnalyticsWorkbenchService.getWorkbenchExtendedCharts());
    }

}
