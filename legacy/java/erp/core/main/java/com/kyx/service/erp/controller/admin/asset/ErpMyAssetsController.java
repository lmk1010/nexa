package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyOwnedAssetsPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyOwnedAssetsRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyCheckoutPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyCheckoutRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyReturnPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyReturnRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyTransferPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyTransferRespVO;
import com.kyx.service.erp.service.asset.ErpMyAssetsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - ERP 我的资产")
@RestController
@RequestMapping("/erp/my-assets")
@Validated
public class ErpMyAssetsController {

    @Resource
    private ErpMyAssetsService myAssetsService;

    @GetMapping("/owned-page/{userId}")
    @Operation(summary = "获得我拥有的资产分页")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<PageResult<ErpMyOwnedAssetsRespVO>> getMyOwnedAssetsPage(@PathVariable Long userId, @Valid ErpMyOwnedAssetsPageReqVO pageReqVO) {
        PageResult<ErpMyOwnedAssetsRespVO> pageResult = myAssetsService.getMyOwnedAssetsPage(userId, pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/checkout-page/{userId}")
    @Operation(summary = "获得我申请领用的资产分页")
    @PreAuthorize("@ss.hasPermission('erp:asset-checkout:query')")
    public CommonResult<PageResult<ErpMyCheckoutRespVO>> getMyCheckoutPage(@PathVariable Long userId, @Valid ErpMyCheckoutPageReqVO pageReqVO) {
        PageResult<ErpMyCheckoutRespVO> pageResult = myAssetsService.getMyCheckoutPage(userId, pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/return-page/{userId}")
    @Operation(summary = "获得我的归还记录分页")
    @PreAuthorize("@ss.hasPermission('erp:asset-return:query')")
    public CommonResult<PageResult<ErpMyReturnRespVO>> getMyReturnPage(@PathVariable Long userId, @Valid ErpMyReturnPageReqVO pageReqVO) {
        PageResult<ErpMyReturnRespVO> pageResult = myAssetsService.getMyReturnPage(userId, pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/transfer-page/{userId}")
    @Operation(summary = "获得我转移的资产分页")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:query')")
    public CommonResult<PageResult<ErpMyTransferRespVO>> getMyTransferPage(@PathVariable Long userId, @Valid ErpMyTransferPageReqVO pageReqVO) {
        PageResult<ErpMyTransferRespVO> pageResult = myAssetsService.getMyTransferPage(userId, pageReqVO);
        return success(pageResult);
    }

} 