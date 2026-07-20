package com.kyx.service.erp.service.purchase;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.purchase.vo.request.ErpPurchaseRequestPageReqVO;
import com.kyx.service.erp.controller.admin.purchase.vo.request.ErpPurchaseRequestSaveReqVO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseRequestDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 采购申请 Service 接口
 *
 * @author MK
 */
public interface ErpPurchaseRequestService {

    /**
     * 创建采购申请
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPurchaseRequest(@Valid ErpPurchaseRequestSaveReqVO createReqVO);

    /**
     * 创建采购申请并提交（发起BPM流程）
     *
     * @param userId 用户编号
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPurchaseRequestAndSubmit(Long userId, @Valid ErpPurchaseRequestSaveReqVO createReqVO);

    /**
     * 更新采购申请
     *
     * @param updateReqVO 更新信息
     */
    void updatePurchaseRequest(@Valid ErpPurchaseRequestSaveReqVO updateReqVO);

    /**
     * 删除采购申请
     *
     * @param id 编号
     */
    void deletePurchaseRequest(Long id);

    /**
     * 获得采购申请
     *
     * @param id 编号
     * @return 采购申请
     */
    ErpPurchaseRequestDO getPurchaseRequest(Long id);

    /**
     * 获得采购申请列表
     *
     * @param ids 编号
     * @return 采购申请列表
     */
    List<ErpPurchaseRequestDO> getPurchaseRequestList(Collection<Long> ids);

    /**
     * 获得采购申请分页
     *
     * @param pageReqVO 分页查询
     * @return 采购申请分页
     */
    PageResult<ErpPurchaseRequestDO> getPurchaseRequestPage(ErpPurchaseRequestPageReqVO pageReqVO);

    /**
     * 提交采购申请
     *
     * @param id 编号
     * @param userId 用户编号
     */
    void submitPurchaseRequest(Long id, Long userId);

    /**
     * 更新采购申请状态
     *
     * @param id 编号
     * @param status 状态
     */
    void updatePurchaseRequestStatus(Long id, Integer status);

    /**
     * 更新采购申请状态
     *
     * @param id 编号
     * @param status 状态
     * @param reason 审核原因
     */
    void updatePurchaseRequestStatus(Long id, Integer status, String reason);

    /**
     * 更新采购申请的BMP流程状态
     *
     * @param id 编号
     * @param status 业务状态
     * @param bmpStatus BMP流程状态
     */
    void updatePurchaseRequestBmpStatus(Long id, Integer status, Integer bmpStatus);

} 