package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.erp.controller.admin.asset.vo.redistribution.ErpAssetRedistributionPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.redistribution.ErpAssetRedistributionRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.redistribution.ErpAssetRedistributionSaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetRedistributionDO;
import com.kyx.service.erp.service.asset.ErpAssetRedistributionService;
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

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.CREATE;
import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.DELETE;
import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.UPDATE;
import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertList;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - ERP 资产调拨")
@RestController
@RequestMapping("/erp/asset-redistribution")
@Validated
public class ErpAssetRedistributionController {

    @Resource
    private ErpAssetRedistributionService assetRedistributionService;

    @PostMapping("/create")
    @Operation(summary = "创建资产调拨记录")
    @PreAuthorize("@ss.hasPermission('erp:asset-redistribution:create')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<Long> createAssetRedistribution(@Valid @RequestBody ErpAssetRedistributionSaveReqVO createReqVO) {
        return success(assetRedistributionService.createAssetRedistribution(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交资产调拨记录（发起BPM流程）")
    @PreAuthorize("@ss.hasPermission('erp:asset-redistribution:create')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<Long> createAssetRedistributionAndSubmit(@Valid @RequestBody ErpAssetRedistributionSaveReqVO createReqVO) {
        return success(assetRedistributionService.createAssetRedistributionAndSubmit(getLoginUserId(), createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产调拨记录")
    @PreAuthorize("@ss.hasPermission('erp:asset-redistribution:update')")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> updateAssetRedistribution(@Valid @RequestBody ErpAssetRedistributionSaveReqVO updateReqVO) {
        assetRedistributionService.updateAssetRedistribution(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产调拨记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:asset-redistribution:delete')")
    @ApiAccessLog(operateType = DELETE)
    public CommonResult<Boolean> deleteAssetRedistribution(@RequestParam("id") Long id) {
        assetRedistributionService.deleteAssetRedistribution(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产调拨记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-redistribution:query')")
    public CommonResult<ErpAssetRedistributionRespVO> getAssetRedistribution(@RequestParam("id") Long id) {
        ErpAssetRedistributionRespVO redistribution = assetRedistributionService.getAssetRedistributionDetail(id);
        return success(redistribution);
    }

    @GetMapping("/page")
    @Operation(summary = "获得资产调拨记录分页")
    @PreAuthorize("@ss.hasPermission('erp:asset-redistribution:query')")
    public CommonResult<PageResult<ErpAssetRedistributionRespVO>> getAssetRedistributionPage(@Valid ErpAssetRedistributionPageReqVO pageReqVO) {
        PageResult<ErpAssetRedistributionRespVO> pageResult = assetRedistributionService.getAssetRedistributionPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/list-by-dept")
    @Operation(summary = "获得指定部门的调拨记录列表")
    @Parameter(name = "deptId", description = "部门编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-redistribution:query')")
    public CommonResult<List<ErpAssetRedistributionRespVO>> getAssetRedistributionListByDeptId(@RequestParam("deptId") Long deptId) {
        List<ErpAssetRedistributionRespVO> list = assetRedistributionService.getAssetRedistributionListByDeptId(deptId);
        return success(list);
    }

    @PostMapping("/approve")
    @Operation(summary = "审批资产调拨申请")
    @PreAuthorize("@ss.hasPermission('erp:asset-redistribution:approve')")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> approveAssetRedistribution(
            @RequestParam("redistributionId") Long redistributionId,
            @RequestParam("approvalStatus") Integer approvalStatus,
            @RequestParam(value = "approvalRemark", required = false) String approvalRemark) {
        assetRedistributionService.approveAssetRedistribution(redistributionId, approvalStatus, approvalRemark);
        return success(true);
    }

    @PostMapping("/confirm-receive")
    @Operation(summary = "确认接收资产调拨")
    @PreAuthorize("@ss.hasPermission('erp:asset-redistribution:confirm')")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> confirmReceiveAssetRedistribution(
            @RequestParam("redistributionId") Long redistributionId,
            @RequestParam(value = "confirmRemark", required = false) String confirmRemark) {
        assetRedistributionService.confirmReceiveAssetRedistribution(redistributionId, confirmRemark);
        return success(true);
    }

    @GetMapping("/can-redistribute")
    @Operation(summary = "检查资产是否可以调拨")
    @PreAuthorize("@ss.hasPermission('erp:asset-redistribution:query')")
    public CommonResult<Boolean> canRedistributeAssets(@RequestParam("assetIds") List<Long> assetIds) {
        boolean canRedistribute = assetRedistributionService.canRedistributeAssets(assetIds);
        return success(canRedistribute);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出资产调拨记录 Excel")
    @PreAuthorize("@ss.hasPermission('erp:asset-redistribution:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAssetRedistributionExcel(@Valid ErpAssetRedistributionPageReqVO pageReqVO,
                                               HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpAssetRedistributionRespVO> list = assetRedistributionService.getAssetRedistributionPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "资产调拨记录.xls", "数据", ErpAssetRedistributionRespVO.class,
                convertList(list, redistribution -> redistribution));
    }
} 