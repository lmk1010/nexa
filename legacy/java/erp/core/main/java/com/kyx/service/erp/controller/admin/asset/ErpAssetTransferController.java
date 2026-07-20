package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferUserSearchReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferUserSearchRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetTransferDO;
import com.kyx.service.erp.service.asset.ErpAssetTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.CREATE;
import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.DELETE;
import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.UPDATE;
import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - ERP 资产转移")
@RestController
@RequestMapping("/erp/asset-transfer")
@Validated
public class ErpAssetTransferController {

    @Resource
    private ErpAssetTransferService assetTransferService;

    @PostMapping("/create")
    @Operation(summary = "创建资产转移记录")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:create')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<Long> createAssetTransfer(@Valid @RequestBody ErpAssetTransferSaveReqVO createReqVO) {
        return success(assetTransferService.createAssetTransfer(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交资产转移记录（发起BPM流程）")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:create')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<Long> createAssetTransferAndSubmit(@Valid @RequestBody ErpAssetTransferSaveReqVO createReqVO) {
        return success(assetTransferService.createAssetTransferAndSubmit(getLoginUserId(), createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产转移记录")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:update')")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> updateAssetTransfer(@Valid @RequestBody ErpAssetTransferSaveReqVO updateReqVO) {
        assetTransferService.updateAssetTransfer(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产转移记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:delete')")
    @ApiAccessLog(operateType = DELETE)
    public CommonResult<Boolean> deleteAssetTransfer(@RequestParam("id") Long id) {
        assetTransferService.deleteAssetTransfer(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产转移记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:query')")
    public CommonResult<ErpAssetTransferRespVO> getAssetTransfer(@RequestParam("id") Long id) {
        ErpAssetTransferDO transfer = assetTransferService.getAssetTransfer(id);
        if (transfer == null) {
            return success(null);
        }
        // TODO: 这里需要转换为RespVO，暂时返回null
        return success(null);
    }

    @GetMapping("/page")
    @Operation(summary = "获得资产转移记录分页")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:query')")
    public CommonResult<PageResult<ErpAssetTransferRespVO>> getAssetTransferPage(@Valid ErpAssetTransferPageReqVO pageReqVO) {
        PageResult<ErpAssetTransferRespVO> pageResult = assetTransferService.getAssetTransferPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/list-by-asset")
    @Operation(summary = "获得指定资产的转移记录列表")
    @Parameter(name = "assetId", description = "资产编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:query')")
    public CommonResult<List<ErpAssetTransferRespVO>> getAssetTransferListByAssetId(@RequestParam("assetId") Long assetId) {
        List<ErpAssetTransferRespVO> list = assetTransferService.getAssetTransferListByAssetId(assetId);
        return success(list);
    }

    @GetMapping("/list-by-from-user")
    @Operation(summary = "获得指定用户发起的转移记录列表")
    @Parameter(name = "userId", description = "用户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:query')")
    public CommonResult<List<ErpAssetTransferRespVO>> getAssetTransferListByFromUserId(@RequestParam("userId") Long userId) {
        List<ErpAssetTransferRespVO> list = assetTransferService.getAssetTransferListByFromUserId(userId);
        return success(list);
    }

    @GetMapping("/list-by-to-user")
    @Operation(summary = "获得指定用户接收的转移记录列表")
    @Parameter(name = "userId", description = "用户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:query')")
    public CommonResult<List<ErpAssetTransferRespVO>> getAssetTransferListByToUserId(@RequestParam("userId") Long userId) {
        List<ErpAssetTransferRespVO> list = assetTransferService.getAssetTransferListByToUserId(userId);
        return success(list);
    }

    @PostMapping("/approve")
    @Operation(summary = "审批资产转移申请")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:approve')")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> approveAssetTransfer(
            @RequestParam("transferId") Long transferId,
            @RequestParam("approvalStatus") Integer approvalStatus,
            @RequestParam(value = "approvalRemark", required = false) String approvalRemark) {
        assetTransferService.approveAssetTransfer(transferId, approvalStatus, approvalRemark);
        return success(true);
    }

    @PostMapping("/confirm-receive")
    @Operation(summary = "确认接收资产转移")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:confirm')")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> confirmReceiveAssetTransfer(
            @RequestParam("transferId") Long transferId,
            @RequestParam(value = "confirmRemark", required = false) String confirmRemark) {
        assetTransferService.confirmReceiveAssetTransfer(transferId, confirmRemark);
        return success(true);
    }

    @GetMapping("/search-users")
    @Operation(summary = "搜索用户")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:query')")
    public CommonResult<List<ErpAssetTransferUserSearchRespVO>> searchUsers(@Valid ErpAssetTransferUserSearchReqVO searchReqVO) {
        List<ErpAssetTransferUserSearchRespVO> list = assetTransferService.searchUsers(searchReqVO);
        return success(list);
    }

    @GetMapping("/can-transfer")
    @Operation(summary = "检查资产是否可以转移")
    @Parameter(name = "assetId", description = "资产编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-transfer:query')")
    public CommonResult<Boolean> canTransferAsset(@RequestParam("assetId") Long assetId) {
        boolean canTransfer = assetTransferService.canTransferAsset(assetId);
        return success(canTransfer);
    }

} 