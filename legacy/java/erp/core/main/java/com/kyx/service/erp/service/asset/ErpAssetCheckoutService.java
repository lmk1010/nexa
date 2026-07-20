package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetCheckoutPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetCheckoutRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetCheckoutSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetReturnReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCheckoutDO;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 资产领用记录 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetCheckoutService {

    /**
     * 创建资产领用记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCheckout(@Valid ErpAssetCheckoutSaveReqVO createReqVO);

    /**
     * 创建资产领用记录并提交（发起BPM流程）
     *
     * @param userId 用户编号
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCheckoutAndSubmit(Long userId, @Valid ErpAssetCheckoutSaveReqVO createReqVO);

    /**
     * 更新资产领用记录
     *
     * @param updateReqVO 更新信息
     */
    void updateCheckout(@Valid ErpAssetCheckoutSaveReqVO updateReqVO);

    /**
     * 删除资产领用记录
     *
     * @param id 编号
     */
    void deleteCheckout(Long id);

    /**
     * 获得资产领用记录
     *
     * @param id 编号
     * @return 资产领用记录
     */
    ErpAssetCheckoutDO getCheckout(Long id);

    /**
     * 获得资产领用记录分页
     *
     * @param pageReqVO 分页查询
     * @return 资产领用记录分页
     */
    PageResult<ErpAssetCheckoutRespVO> getCheckoutPage(ErpAssetCheckoutPageReqVO pageReqVO);

    /**
     * 获得资产领用记录列表
     *
     * @param assetId 资产编号
     * @return 资产领用记录列表
     */
    List<ErpAssetCheckoutRespVO> getCheckoutListByAssetId(Long assetId);

    /**
     * 获得用户的资产领用记录列表
     *
     * @param userId 用户编号
     * @return 资产领用记录列表
     */
    List<ErpAssetCheckoutRespVO> getCheckoutListByUserId(Long userId);

    /**
     * 资产归还
     *
     * @param returnReqVO 归还信息
     */
    void returnAsset(@Valid ErpAssetReturnReqVO returnReqVO);

    /**
     * 审批资产领用申请
     *
     * @param checkoutId 领用记录编号
     * @param approvalStatus 审批状态
     * @param approvalRemark 审批备注
     */
    void approveCheckout(Long checkoutId, Integer approvalStatus, String approvalRemark);

    /**
     * 获取逾期未还的资产列表
     *
     * @return 逾期资产列表
     */
    List<ErpAssetCheckoutRespVO> getOverdueCheckoutList();

    /**
     * 检查资产是否可以领用
     *
     * @param assetId 资产编号
     * @return 是否可以领用
     */
    boolean canCheckout(Long assetId);

    /**
     * 更新资产领用BMP状态
     *
     * @param id 编号
     * @param approvalStatus 审批状态
     * @param bmpStatus BMP状态
     */
    void updateCheckoutBmpStatus(Long id, Integer approvalStatus, Integer bmpStatus);

} 