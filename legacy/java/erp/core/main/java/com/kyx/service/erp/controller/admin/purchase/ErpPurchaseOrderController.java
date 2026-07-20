package com.kyx.service.erp.controller.admin.purchase;

import cn.hutool.core.collection.CollUtil;
import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.collection.MapUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.erp.controller.admin.product.vo.product.ErpProductRespVO;
import com.kyx.service.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import com.kyx.service.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderRespVO;
import com.kyx.service.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpSupplierDO;
import com.kyx.service.erp.service.product.ErpProductService;
import com.kyx.service.erp.service.purchase.ErpPurchaseOrderService;
import com.kyx.service.erp.service.purchase.ErpSupplierService;
import com.kyx.service.erp.service.stock.ErpStockService;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertMultiMap;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - ERP 采购订单")
@RestController
@RequestMapping("/erp/purchase-order")
@Validated
public class ErpPurchaseOrderController {

    @Resource
    private ErpPurchaseOrderService purchaseOrderService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建采购订单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:create')")
    public CommonResult<Long> createPurchaseOrder(@Valid @RequestBody ErpPurchaseOrderSaveReqVO createReqVO) {
        return success(purchaseOrderService.createPurchaseOrder(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交采购订单（发起BPM流程）")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:create')")
    public CommonResult<Long> createPurchaseOrderAndSubmit(@Valid @RequestBody ErpPurchaseOrderSaveReqVO createReqVO) {
        return success(purchaseOrderService.createPurchaseOrderAndSubmit(getLoginUserId(), createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购订单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:update')")
    public CommonResult<Boolean> updatePurchaseOrder(@Valid @RequestBody ErpPurchaseOrderSaveReqVO updateReqVO) {
        purchaseOrderService.updatePurchaseOrder(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新采购订单的状态")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:update-status')")
    public CommonResult<Boolean> updatePurchaseOrderStatus(@RequestParam("id") Long id,
                                                           @RequestParam("status") Integer status) {
        purchaseOrderService.updatePurchaseOrderStatus(id, status);
        return success(true);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交采购订单（发起BPM流程）")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:submit')")
    public CommonResult<Boolean> submitPurchaseOrder(@RequestParam("id") Long id) {
        purchaseOrderService.submitPurchaseOrder(id, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购订单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:delete')")
    public CommonResult<Boolean> deletePurchaseOrder(@RequestParam("ids") List<Long> ids) {
        purchaseOrderService.deletePurchaseOrder(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购订单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:query')")
    public CommonResult<ErpPurchaseOrderRespVO> getPurchaseOrder(@RequestParam("id") Long id) {
        ErpPurchaseOrderDO purchaseOrder = purchaseOrderService.getPurchaseOrder(id);
        if (purchaseOrder == null) {
            return success(null);
        }
        List<ErpPurchaseOrderItemDO> purchaseOrderItemList = purchaseOrderService.getPurchaseOrderItemListByOrderId(id);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseOrderItemList, ErpPurchaseOrderItemDO::getProductId));
        return success(BeanUtils.toBean(purchaseOrder, ErpPurchaseOrderRespVO.class, purchaseOrderVO ->
                purchaseOrderVO.setItems(BeanUtils.toBean(purchaseOrderItemList, ErpPurchaseOrderRespVO.Item.class, item -> {
                    BigDecimal purchaseCount = stockService.getStockCount(item.getProductId());
                    item.setStockCount(purchaseCount != null ? purchaseCount : BigDecimal.ZERO);
                    MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName()));
                }))));
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购订单分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:query')")
    public CommonResult<PageResult<ErpPurchaseOrderRespVO>> getPurchaseOrderPage(@Valid ErpPurchaseOrderPageReqVO pageReqVO) {
        PageResult<ErpPurchaseOrderDO> pageResult = purchaseOrderService.getPurchaseOrderPage(pageReqVO);
        return success(buildPurchaseOrderVOPageResult(pageResult));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购订单 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseOrderExcel(@Valid ErpPurchaseOrderPageReqVO pageReqVO,
                                         HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpPurchaseOrderRespVO> list = buildPurchaseOrderVOPageResult(purchaseOrderService.getPurchaseOrderPage(pageReqVO)).getList();
        // 导出 Excel
        ExcelUtils.write(response, "采购订单.xls", "数据", ErpPurchaseOrderRespVO.class, list);
    }

    private PageResult<ErpPurchaseOrderRespVO> buildPurchaseOrderVOPageResult(PageResult<ErpPurchaseOrderDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        // 1.1 订单项
        List<ErpPurchaseOrderItemDO> purchaseOrderItemList = purchaseOrderService.getPurchaseOrderItemListByOrderIds(
                convertSet(pageResult.getList(), ErpPurchaseOrderDO::getId));
        Map<Long, List<ErpPurchaseOrderItemDO>> purchaseOrderItemMap = convertMultiMap(purchaseOrderItemList, ErpPurchaseOrderItemDO::getOrderId);
        // 1.2 产品信息
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseOrderItemList, ErpPurchaseOrderItemDO::getProductId));
        // 1.3 供应商信息
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseOrderDO::getSupplierId));
        // 1.4 管理员信息
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(pageResult.getList(), purchaseOrder -> Long.parseLong(purchaseOrder.getCreator())));
        // 2. 开始拼接
        return BeanUtils.toBean(pageResult, ErpPurchaseOrderRespVO.class, purchaseOrder -> {
            purchaseOrder.setItems(BeanUtils.toBean(purchaseOrderItemMap.get(purchaseOrder.getId()), ErpPurchaseOrderRespVO.Item.class,
                    item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName()))));
            purchaseOrder.setProductNames(CollUtil.join(purchaseOrder.getItems(), "，", ErpPurchaseOrderRespVO.Item::getProductName));
            MapUtils.findAndThen(supplierMap, purchaseOrder.getSupplierId(), supplier -> purchaseOrder.setSupplierName(supplier.getName()));
            MapUtils.findAndThen(userMap, Long.parseLong(purchaseOrder.getCreator()), user -> purchaseOrder.setCreatorName(user.getNickname()));
        });
    }

}