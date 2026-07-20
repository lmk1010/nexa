package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.overview.*;
import com.kyx.service.erp.service.asset.ErpAssetOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 资产纵览")
@RestController
@RequestMapping("/erp/asset-overview")
@Validated
public class ErpAssetOverviewController {

    @Resource
    private ErpAssetOverviewService assetOverviewService;

    @GetMapping("/statistics")
    @Operation(summary = "获取资产统计数据")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<ErpAssetStatisticsRespVO> getAssetStatistics(
            @Valid ErpAssetStatisticsReqVO reqVO) {
        return success(assetOverviewService.getAssetStatistics(reqVO));
    }

    @GetMapping("/statistics/category")
    @Operation(summary = "获取资产分类统计数据")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<List<ErpAssetCategoryStatisticsRespVO>> getCategoryStatistics(
            @Valid ErpAssetCategoryStatisticsReqVO reqVO) {
        return success(assetOverviewService.getCategoryStatistics(reqVO));
    }

    @GetMapping("/statistics/dept")
    @Operation(summary = "获取部门资产统计数据")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<List<ErpAssetDeptStatisticsRespVO>> getDeptStatistics(
            @Valid ErpAssetDeptStatisticsReqVO reqVO) {
        return success(assetOverviewService.getDeptStatistics(reqVO));
    }

    @GetMapping("/statistics/user")
    @Operation(summary = "获取用户资产统计数据")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<List<ErpAssetUserStatisticsRespVO>> getUserStatistics(
            @Valid ErpAssetUserStatisticsReqVO reqVO) {
        return success(assetOverviewService.getUserStatistics(reqVO));
    }

    @GetMapping("/expiring")
    @Operation(summary = "获取快过期资产列表")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<PageResult<ErpAssetExpiringRespVO>> getExpiringAssets(
            @Valid ErpAssetExpiringPageReqVO pageReqVO) {
        return success(assetOverviewService.getExpiringAssetsPage(pageReqVO));
    }

    @GetMapping("/status-distribution")
    @Operation(summary = "获取资产状态分布统计")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<List<ErpAssetStatusStatisticsRespVO>> getStatusDistribution(
            @Valid ErpAssetStatusStatisticsReqVO reqVO) {
        return success(assetOverviewService.getStatusDistribution(reqVO));
    }
}