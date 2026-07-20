package com.kyx.service.erp.controller.admin.purchase;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.erp.controller.admin.purchase.vo.request.ErpPurchaseRequestPageReqVO;
import com.kyx.service.erp.controller.admin.purchase.vo.request.ErpPurchaseRequestRespVO;
import com.kyx.service.erp.controller.admin.purchase.vo.request.ErpPurchaseRequestSaveReqVO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseRequestDO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseRequestItemDO;
import com.kyx.service.erp.dal.mysql.purchase.ErpPurchaseRequestItemMapper;
import com.kyx.service.erp.service.purchase.ErpPurchaseRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - ERP 采购申请")
@RestController
@RequestMapping("/erp/purchase-request")
@Validated
public class ErpPurchaseRequestController {

    @Resource
    private ErpPurchaseRequestService purchaseRequestService;
    @Resource
    private ErpPurchaseRequestItemMapper purchaseRequestItemMapper;

    @PostMapping("/create")
    @Operation(summary = "创建采购申请")
    @PreAuthorize("@ss.hasPermission('erp:purchase-request:create')")
    public CommonResult<Long> createPurchaseRequest(@Valid @RequestBody ErpPurchaseRequestSaveReqVO createReqVO) {
        return success(purchaseRequestService.createPurchaseRequest(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交采购申请（发起BPM流程）")
    @PreAuthorize("@ss.hasPermission('erp:purchase-request:create')")
    public CommonResult<Long> createPurchaseRequestAndSubmit(@Valid @RequestBody ErpPurchaseRequestSaveReqVO createReqVO) {
        return success(purchaseRequestService.createPurchaseRequestAndSubmit(getLoginUserId(), createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购申请")
    @PreAuthorize("@ss.hasPermission('erp:purchase-request:update')")
    public CommonResult<Boolean> updatePurchaseRequest(@Valid @RequestBody ErpPurchaseRequestSaveReqVO updateReqVO) {
        purchaseRequestService.updatePurchaseRequest(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购申请")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:purchase-request:delete')")
    public CommonResult<Boolean> deletePurchaseRequest(@RequestParam("id") Long id) {
        purchaseRequestService.deletePurchaseRequest(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购申请")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-request:query')")
    public CommonResult<ErpPurchaseRequestRespVO> getPurchaseRequest(@RequestParam("id") Long id) {
        ErpPurchaseRequestDO purchaseRequest = purchaseRequestService.getPurchaseRequest(id);
        if (purchaseRequest == null) {
            return success(null);
        }

        // 1.1 转换为VO
        ErpPurchaseRequestRespVO respVO = BeanUtils.toBean(purchaseRequest, ErpPurchaseRequestRespVO.class);
        
        // 1.2 读取明细
        List<ErpPurchaseRequestItemDO> purchaseRequestItems = purchaseRequestItemMapper.selectListByRequestId(id);
        respVO.setItems(BeanUtils.toBean(purchaseRequestItems, ErpPurchaseRequestRespVO.Item.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购申请分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-request:query')")
    public CommonResult<PageResult<ErpPurchaseRequestRespVO>> getPurchaseRequestPage(@Valid ErpPurchaseRequestPageReqVO pageReqVO) {
        PageResult<ErpPurchaseRequestDO> pageResult = purchaseRequestService.getPurchaseRequestPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpPurchaseRequestRespVO.class));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交采购申请")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:purchase-request:submit')")
    public CommonResult<Boolean> submitPurchaseRequest(@RequestParam("id") Long id) {
        purchaseRequestService.submitPurchaseRequest(id, getLoginUserId());
        return success(true);
    }

    @PostMapping("/audit")
    @Operation(summary = "审核采购申请")
    @Parameter(name = "id", description = "编号", required = true)
    @Parameter(name = "status", description = "状态", required = true)
    @Parameter(name = "reason", description = "审核原因")
    @PreAuthorize("@ss.hasPermission('erp:purchase-request:audit')")
    public CommonResult<Boolean> auditPurchaseRequest(@RequestParam("id") Long id,
                                                      @RequestParam("status") Integer status,
                                                      @RequestParam(value = "reason", required = false) String reason) {
        purchaseRequestService.updatePurchaseRequestStatus(id, status, reason);
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购申请 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-request:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseRequestExcel(@Valid ErpPurchaseRequestPageReqVO pageReqVO,
                                          HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpPurchaseRequestDO> list = purchaseRequestService.getPurchaseRequestPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "采购申请.xls", "数据", ErpPurchaseRequestRespVO.class,
                        BeanUtils.toBean(list, ErpPurchaseRequestRespVO.class));
    }

} 