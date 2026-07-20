package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetCheckoutPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetCheckoutRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetCheckoutSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetReturnReqVO;
import com.kyx.service.erp.service.asset.ErpAssetCheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.*;
import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - ERP 资产领用")
@RestController
@RequestMapping("/erp/asset-checkout")
@Validated
public class ErpAssetCheckoutController {

    @Resource
    private ErpAssetCheckoutService checkoutService;

    @PostMapping("/create")
    @Operation(summary = "创建资产领用记录")
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:create')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<Long> createCheckout(@Valid @RequestBody ErpAssetCheckoutSaveReqVO createReqVO) {
        return success(checkoutService.createCheckout(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交资产领用记录（发起BPM流程）")
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:create')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<Long> createCheckoutAndSubmit(@Valid @RequestBody ErpAssetCheckoutSaveReqVO createReqVO) {
        return success(checkoutService.createCheckoutAndSubmit(getLoginUserId(), createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产领用记录")
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:update')")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> updateCheckout(@Valid @RequestBody ErpAssetCheckoutSaveReqVO updateReqVO) {
        checkoutService.updateCheckout(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产领用记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:delete')")
    @ApiAccessLog(operateType = DELETE)
    public CommonResult<Boolean> deleteCheckout(@RequestParam("id") Long id) {
        checkoutService.deleteCheckout(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产领用记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:query')")
    public CommonResult<ErpAssetCheckoutRespVO> getCheckout(@RequestParam("id") Long id) {
        // 这里需要将DO转换为VO，但为了简化，我们暂时返回基础数据
        // 实际项目中应该在Service中提供对应的VO返回方法
        return success(null); // TODO: 实现DO到VO的转换
    }

    @GetMapping("/page")
    @Operation(summary = "获得资产领用记录分页")
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:query')")
    public CommonResult<PageResult<ErpAssetCheckoutRespVO>> getCheckoutPage(@Valid ErpAssetCheckoutPageReqVO pageReqVO) {
        PageResult<ErpAssetCheckoutRespVO> pageResult = checkoutService.getCheckoutPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/list-by-asset")
    @Operation(summary = "获得指定资产的领用记录列表")
    @Parameter(name = "assetId", description = "资产编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:query')")
    public CommonResult<List<ErpAssetCheckoutRespVO>> getCheckoutListByAssetId(@RequestParam("assetId") Long assetId) {
        List<ErpAssetCheckoutRespVO> list = checkoutService.getCheckoutListByAssetId(assetId);
        return success(list);
    }

    @GetMapping("/list-by-user")
    @Operation(summary = "获得指定用户的领用记录列表")
    @Parameter(name = "userId", description = "用户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:query')")
    public CommonResult<List<ErpAssetCheckoutRespVO>> getCheckoutListByUserId(@RequestParam("userId") Long userId) {
        List<ErpAssetCheckoutRespVO> list = checkoutService.getCheckoutListByUserId(userId);
        return success(list);
    }

    @PostMapping("/return")
    @Operation(summary = "资产归还")
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:return')")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> returnAsset(@Valid @RequestBody ErpAssetReturnReqVO returnReqVO) {
        checkoutService.returnAsset(returnReqVO);
        return success(true);
    }

    @PostMapping("/approve")
    @Operation(summary = "审批资产领用申请")
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:approve')")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> approveCheckout(
            @RequestParam("checkoutId") Long checkoutId,
            @RequestParam("approvalStatus") Integer approvalStatus,
            @RequestParam(value = "approvalRemark", required = false) String approvalRemark) {
        checkoutService.approveCheckout(checkoutId, approvalStatus, approvalRemark);
        return success(true);
    }

    @GetMapping("/overdue-list")
    @Operation(summary = "获取逾期未还的资产列表")
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:query')")
    public CommonResult<List<ErpAssetCheckoutRespVO>> getOverdueCheckoutList() {
        List<ErpAssetCheckoutRespVO> list = checkoutService.getOverdueCheckoutList();
        return success(list);
    }

    @GetMapping("/can-checkout")
    @Operation(summary = "检查资产是否可以领用")
    @Parameter(name = "assetId", description = "资产编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:query')")
    public CommonResult<Boolean> canCheckout(@RequestParam("assetId") Long assetId) {
        boolean canCheckout = checkoutService.canCheckout(assetId);
        return success(canCheckout);
    }

} 