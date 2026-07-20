package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferUserSearchReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferUserSearchRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetTransferDO;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 资产转移记录 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetTransferService {

    /**
     * 创建资产转移记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAssetTransfer(@Valid ErpAssetTransferSaveReqVO createReqVO);

    /**
     * 创建并提交资产转移记录（发起BPM流程）
     *
     * @param userId 用户编号
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAssetTransferAndSubmit(Long userId, @Valid ErpAssetTransferSaveReqVO createReqVO);

    /**
     * 更新资产转移记录
     *
     * @param updateReqVO 更新信息
     */
    void updateAssetTransfer(@Valid ErpAssetTransferSaveReqVO updateReqVO);

    /**
     * 删除资产转移记录
     *
     * @param id 编号
     */
    void deleteAssetTransfer(Long id);

    /**
     * 获得资产转移记录
     *
     * @param id 编号
     * @return 资产转移记录
     */
    ErpAssetTransferDO getAssetTransfer(Long id);

    /**
     * 获得资产转移记录分页
     *
     * @param pageReqVO 分页查询
     * @return 资产转移记录分页
     */
    PageResult<ErpAssetTransferRespVO> getAssetTransferPage(ErpAssetTransferPageReqVO pageReqVO);

    /**
     * 获得资产转移记录列表
     *
     * @param assetId 资产编号
     * @return 资产转移记录列表
     */
    List<ErpAssetTransferRespVO> getAssetTransferListByAssetId(Long assetId);

    /**
     * 获得用户发起的转移记录列表
     *
     * @param fromUserId 用户编号
     * @return 资产转移记录列表
     */
    List<ErpAssetTransferRespVO> getAssetTransferListByFromUserId(Long fromUserId);

    /**
     * 获得用户接收的转移记录列表
     *
     * @param toUserId 用户编号
     * @return 资产转移记录列表
     */
    List<ErpAssetTransferRespVO> getAssetTransferListByToUserId(Long toUserId);

    /**
     * 审批资产转移申请
     *
     * @param transferId 转移记录编号
     * @param approvalStatus 审批状态
     * @param approvalRemark 审批备注
     */
    void approveAssetTransfer(Long transferId, Integer approvalStatus, String approvalRemark);

    /**
     * 确认接收资产转移
     *
     * @param transferId 转移记录编号
     * @param confirmRemark 确认备注
     */
    void confirmReceiveAssetTransfer(Long transferId, String confirmRemark);

    /**
     * 搜索用户
     *
     * @param searchReqVO 搜索条件
     * @return 用户列表
     */
    List<ErpAssetTransferUserSearchRespVO> searchUsers(ErpAssetTransferUserSearchReqVO searchReqVO);

    /**
     * 检查资产是否可以转移
     *
     * @param assetId 资产编号
     * @return 是否可以转移
     */
    boolean canTransferAsset(Long assetId);

    /**
     * 更新资产转移的BMP状态
     *
     * @param transferId 转移记录编号
     * @param approvalStatus 审批状态
     * @param bmpStatus BMP状态
     */
    void updateAssetTransferBmpStatus(Long transferId, Integer approvalStatus, Integer bmpStatus);

} 