package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.assetinput.ErpAssetInputPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetinput.ErpAssetInputRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetinput.ErpAssetInputSaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetInputDO;

import javax.validation.Valid;

/**
 * ERP 资产录入申请 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetInputService {

    /**
     * 创建资产录入申请
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAssetInput(@Valid ErpAssetInputSaveReqVO createReqVO);

    /**
     * 创建资产录入申请并提交工作流
     *
     * @param userId 用户编号
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAssetInputAndSubmit(Long userId, @Valid ErpAssetInputSaveReqVO createReqVO);

    /**
     * 更新资产录入申请
     *
     * @param updateReqVO 更新信息
     */
    void updateAssetInput(@Valid ErpAssetInputSaveReqVO updateReqVO);

    /**
     * 删除资产录入申请
     *
     * @param id 编号
     */
    void deleteAssetInput(Long id);

    /**
     * 获得资产录入申请
     *
     * @param id 编号
     * @return 资产录入申请
     */
    ErpAssetInputDO getAssetInput(Long id);

    /**
     * 获得资产录入申请分页
     *
     * @param pageReqVO 分页查询
     * @return 资产录入申请分页
     */
    PageResult<ErpAssetInputRespVO> getAssetInputVOPage(ErpAssetInputPageReqVO pageReqVO);

    /**
     * 根据BMP流程实例ID获取资产录入申请
     *
     * @param bmpProcessInstanceId BMP流程实例ID
     * @return 资产录入申请
     */
    ErpAssetInputDO getAssetInputByBmpProcessInstanceId(String bmpProcessInstanceId);

    /**
     * 审批通过回调 - 创建正式资产记录
     *
     * @param assetInputId 资产录入申请ID
     * @param approverUserId 审批人用户ID
     * @param approverUserName 审批人姓名
     * @param approvalRemark 审批备注
     * @return 创建的资产ID
     */
    Long approveAssetInput(Long assetInputId, Long approverUserId, String approverUserName, String approvalRemark);

    /**
     * 审批拒绝回调
     *
     * @param assetInputId 资产录入申请ID
     * @param approverUserId 审批人用户ID
     * @param approverUserName 审批人姓名
     * @param rejectReason 拒绝原因
     */
    void rejectAssetInput(Long assetInputId, Long approverUserId, String approverUserName, String rejectReason);

    /**
     * 更新BMP流程状态
     *
     * @param assetInputId 资产录入申请ID
     * @param bmpStatus BMP流程状态
     */
    void updateBmpStatus(Long assetInputId, Integer bmpStatus);

} 