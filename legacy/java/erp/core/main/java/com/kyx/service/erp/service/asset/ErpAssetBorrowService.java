package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.api.asset.vo.borrow.ErpAssetBorrowSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.borrow.ErpAssetBorrowPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.borrow.ErpAssetBorrowRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetBorrowDO;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 资产借用记录 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetBorrowService {

    /**
     * 创建资产借用记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createBorrow(@Valid ErpAssetBorrowSaveReqVO createReqVO);

    /**
     * 创建并提交资产借用记录（发起BPM流程）
     *
     * @param userId 用户编号
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createBorrowAndSubmit(Long userId, @Valid ErpAssetBorrowSaveReqVO createReqVO);

    /**
     * 更新资产借用记录
     *
     * @param updateReqVO 更新信息
     */
    void updateBorrow(@Valid ErpAssetBorrowSaveReqVO updateReqVO);

    /**
     * 删除资产借用记录
     *
     * @param id 编号
     */
    void deleteBorrow(Long id);

    /**
     * 获得资产借用记录
     *
     * @param id 编号
     * @return 资产借用记录
     */
    ErpAssetBorrowDO getBorrow(Long id);

    /**
     * 获得资产借用记录分页
     *
     * @param pageReqVO 分页查询
     * @return 资产借用记录分页
     */
    PageResult<ErpAssetBorrowRespVO> getBorrowPage(ErpAssetBorrowPageReqVO pageReqVO);

    /**
     * 获得资产借用记录列表（用于Excel导出）
     *
     * @param exportReqVO 查询条件
     * @return 资产借用记录列表
     */
    List<ErpAssetBorrowRespVO> getBorrowList(ErpAssetBorrowPageReqVO exportReqVO);

    /**
     * 检查资产是否可以借用
     *
     * @param assetId 资产编号
     * @return 是否可以借用
     */
    boolean canBorrow(Long assetId);

    /**
     * 资产归还
     *
     * @param id 借用记录编号
     * @param returnCondition 归还状态
     * @param returnRemark 归还备注
     */
    void returnAsset(Long id, Integer returnCondition, String returnRemark);

    /**
     * 更新借用记录的BMP状态
     *
     * @param id 借用记录编号
     * @param approvalStatus 审批状态
     * @param bmpStatus BMP状态
     */
    void updateBorrowBmpStatus(Long id, Integer approvalStatus, Integer bmpStatus);

    /**
     * 获取逾期未还的借用记录
     *
     * @return 逾期借用记录列表
     */
    List<ErpAssetBorrowDO> getOverdueBorrows();

    /**
     * 更新逾期状态
     *
     * @param id 借用记录编号
     */
    void updateOverdueStatus(Long id);

    /**
     * 根据BMP流程实例ID获得资产借用记录
     *
     * @param bmpProcessInstanceId BMP流程实例ID
     * @return 资产借用记录
     */
    ErpAssetBorrowDO getBorrowByBmpProcessInstanceId(String bmpProcessInstanceId);
} 