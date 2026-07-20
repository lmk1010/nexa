package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.api.asset.vo.scrapped.ErpAssetScrappedSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.scrapped.ErpAssetScrappedPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.scrapped.ErpAssetScrappedRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetScrappedDO;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 资产报废 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetScrappedService {

    /**
     * 创建资产报废记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createScrapped(@Valid ErpAssetScrappedSaveReqVO createReqVO);

    /**
     * 创建并提交资产报废记录（发起BPM流程）
     *
     * @param userId      用户编号
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createScrappedAndSubmit(Long userId, @Valid ErpAssetScrappedSaveReqVO createReqVO);

    /**
     * 更新资产报废记录
     *
     * @param updateReqVO 更新信息
     */
    void updateScrapped(@Valid ErpAssetScrappedSaveReqVO updateReqVO);

    /**
     * 删除资产报废记录
     *
     * @param id 编号
     */
    void deleteScrapped(Long id);

    /**
     * 获得资产报废记录
     *
     * @param id 编号
     * @return 资产报废记录
     */
    ErpAssetScrappedDO getScrapped(Long id);

    /**
     * 获得资产报废记录分页
     *
     * @param pageReqVO 分页查询
     * @return 资产报废记录分页
     */
    PageResult<ErpAssetScrappedRespVO> getScrappedPage(ErpAssetScrappedPageReqVO pageReqVO);

    /**
     * 获得资产报废记录列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 资产报废记录列表
     */
    List<ErpAssetScrappedRespVO> getScrappedList(ErpAssetScrappedPageReqVO exportReqVO);

    /**
     * 根据BMP流程实例ID获得资产报废记录
     *
     * @param bmpProcessInstanceId BMP流程实例ID
     * @return 资产报废记录
     */
    ErpAssetScrappedDO getScrappedByBmpProcessInstanceId(String bmpProcessInstanceId);

    /**
     * 更新资产报废BMP状态
     *
     * @param id             编号
     * @param approvalStatus 审批状态
     * @param bmpStatus      BMP状态
     */
    void updateScrappedBmpStatus(Long id, Integer approvalStatus, Integer bmpStatus);

    /**
     * 完成资产报废处理
     *
     * @param id 编号
     * @param processingMethod 处理方式
     * @param disposalRevenue 处置收入
     * @param remark 备注
     */
    void completeScrapped(Long id, String processingMethod, java.math.BigDecimal disposalRevenue, String remark);
} 