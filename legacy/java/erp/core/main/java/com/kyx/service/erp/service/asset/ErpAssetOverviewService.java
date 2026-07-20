package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.overview.*;

import java.util.List;

/**
 * ERP 资产纵览 Service 接口
 *
 * @author KYX
 */
public interface ErpAssetOverviewService {

    /**
     * 获取资产统计数据
     *
     * @param reqVO 请求参数
     * @return 统计数据
     */
    ErpAssetStatisticsRespVO getAssetStatistics(ErpAssetStatisticsReqVO reqVO);

    /**
     * 获取资产分类统计数据
     *
     * @param reqVO 请求参数
     * @return 分类统计数据列表
     */
    List<ErpAssetCategoryStatisticsRespVO> getCategoryStatistics(ErpAssetCategoryStatisticsReqVO reqVO);

    /**
     * 获取部门资产统计数据
     *
     * @param reqVO 请求参数
     * @return 部门统计数据列表
     */
    List<ErpAssetDeptStatisticsRespVO> getDeptStatistics(ErpAssetDeptStatisticsReqVO reqVO);

    /**
     * 获取用户资产统计数据
     *
     * @param reqVO 请求参数
     * @return 用户统计数据列表
     */
    List<ErpAssetUserStatisticsRespVO> getUserStatistics(ErpAssetUserStatisticsReqVO reqVO);

    /**
     * 获取快过期资产分页
     *
     * @param pageReqVO 分页请求参数
     * @return 快过期资产分页数据
     */
    PageResult<ErpAssetExpiringRespVO> getExpiringAssetsPage(ErpAssetExpiringPageReqVO pageReqVO);

    /**
     * 获取资产状态分布统计
     *
     * @param reqVO 请求参数
     * @return 状态分布统计数据列表
     */
    List<ErpAssetStatusStatisticsRespVO> getStatusDistribution(ErpAssetStatusStatisticsReqVO reqVO);

}