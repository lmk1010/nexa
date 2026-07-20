package com.kyx.service.erp.service.purchase;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import com.kyx.service.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import javax.validation.Valid;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ERP 采购订单 Service 接口
 *
 * @author MK
 */
public interface ErpPurchaseOrderService {

    /**
     * 创建采购订单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPurchaseOrder(@Valid ErpPurchaseOrderSaveReqVO createReqVO);

    /**
     * 创建采购订单并提交（发起BPM流程）
     *
     * @param userId 用户编号
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPurchaseOrderAndSubmit(Long userId, @Valid ErpPurchaseOrderSaveReqVO createReqVO);

    /**
     * 更新采购订单
     *
     * @param updateReqVO 更新信息
     */
    void updatePurchaseOrder(@Valid ErpPurchaseOrderSaveReqVO updateReqVO);

    /**
     * 更新采购订单的状态
     *
     * @param id 编号
     * @param status 状态
     */
    void updatePurchaseOrderStatus(Long id, Integer status);

    /**
     * 提交采购订单（发起BPM流程）
     *
     * @param id 编号
     * @param userId 用户编号
     */
    void submitPurchaseOrder(Long id, Long userId);

    /**
     * 更新采购订单BMP状态
     *
     * @param id 编号
     * @param status 状态
     * @param bmpStatus BMP状态
     */
    void updatePurchaseOrderBmpStatus(Long id, Integer status, Integer bmpStatus);

    /**
     * 更新采购订单的入库数量
     *
     * @param id 编号
     * @param inCountMap 入库数量 Map：key 采购订单项编号；value 入库数量
     */
    void updatePurchaseOrderInCount(Long id, Map<Long, BigDecimal> inCountMap);

    /**
     * 更新采购订单的退货数量
     *
     * @param orderId 编号
     * @param returnCountMap 退货数量 Map：key 采购订单项编号；value 退货数量
     */
    void updatePurchaseOrderReturnCount(Long orderId, Map<Long, BigDecimal> returnCountMap);

    /**
     * 删除采购订单
     *
     * @param ids 编号数组
     */
    void deletePurchaseOrder(List<Long> ids);

    /**
     * 获得采购订单
     *
     * @param id 编号
     * @return 采购订单
     */
    ErpPurchaseOrderDO getPurchaseOrder(Long id);

    /**
     * 校验采购订单，已经审核通过
     *
     * @param id 编号
     * @return 采购订单
     */
    ErpPurchaseOrderDO validatePurchaseOrder(Long id);

    /**
     * 获得采购订单分页
     *
     * @param pageReqVO 分页查询
     * @return 采购订单分页
     */
    PageResult<ErpPurchaseOrderDO> getPurchaseOrderPage(ErpPurchaseOrderPageReqVO pageReqVO);

    // ==================== 采购订单项 ====================

    /**
     * 获得采购订单项列表
     *
     * @param orderId 采购订单编号
     * @return 采购订单项列表
     */
    List<ErpPurchaseOrderItemDO> getPurchaseOrderItemListByOrderId(Long orderId);

    /**
     * 获得采购订单项 List
     *
     * @param orderIds 采购订单编号数组
     * @return 采购订单项 List
     */
    List<ErpPurchaseOrderItemDO> getPurchaseOrderItemListByOrderIds(Collection<Long> orderIds);

}