package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.redistribution.ErpAssetRedistributionPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.redistribution.ErpAssetRedistributionRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.redistribution.ErpAssetRedistributionSaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetRedistributionDO;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 资产调拨记录 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetRedistributionService {

    /**
     * 创建资产调拨记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAssetRedistribution(@Valid ErpAssetRedistributionSaveReqVO createReqVO);

    /**
     * 创建并提交资产调拨记录（发起BPM流程）
     *
     * @param userId 用户编号
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAssetRedistributionAndSubmit(Long userId, @Valid ErpAssetRedistributionSaveReqVO createReqVO);

    /**
     * 更新资产调拨记录
     *
     * @param updateReqVO 更新信息
     */
    void updateAssetRedistribution(@Valid ErpAssetRedistributionSaveReqVO updateReqVO);

    /**
     * 删除资产调拨记录
     *
     * @param id 编号
     */
    void deleteAssetRedistribution(Long id);

    /**
     * 获得资产调拨记录
     *
     * @param id 编号
     * @return 资产调拨记录
     */
    ErpAssetRedistributionDO getAssetRedistribution(Long id);

    /**
     * 获得资产调拨记录分页
     *
     * @param pageReqVO 分页查询
     * @return 资产调拨记录分页
     */
    PageResult<ErpAssetRedistributionRespVO> getAssetRedistributionPage(ErpAssetRedistributionPageReqVO pageReqVO);

    /**
     * 获得资产调拨记录详情
     *
     * @param id 编号
     * @return 资产调拨记录详情
     */
    ErpAssetRedistributionRespVO getAssetRedistributionDetail(Long id);

    /**
     * 获得指定部门的调拨记录列表
     *
     * @param deptId 部门编号
     * @return 资产调拨记录列表
     */
    List<ErpAssetRedistributionRespVO> getAssetRedistributionListByDeptId(Long deptId);

    /**
     * 审批资产调拨申请
     *
     * @param redistributionId 调拨记录编号
     * @param approvalStatus 审批状态
     * @param approvalRemark 审批备注
     */
    void approveAssetRedistribution(Long redistributionId, Integer approvalStatus, String approvalRemark);

    /**
     * 确认接收资产调拨
     *
     * @param redistributionId 调拨记录编号
     * @param confirmRemark 确认备注
     */
    void confirmReceiveAssetRedistribution(Long redistributionId, String confirmRemark);

    /**
     * 检查资产是否可以调拨
     *
     * @param assetIds 资产编号列表
     * @return 是否可以调拨
     */
    boolean canRedistributeAssets(List<Long> assetIds);

    /**
     * 更新资产调拨的BMP状态
     *
     * @param redistributionId 调拨记录编号
     * @param approvalStatus 审批状态
     * @param bmpStatus BMP状态
     */
    void updateAssetRedistributionBmpStatus(Long redistributionId, Integer approvalStatus, Integer bmpStatus, Long approverUserId);
} 