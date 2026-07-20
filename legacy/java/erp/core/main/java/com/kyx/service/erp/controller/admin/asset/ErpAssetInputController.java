package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.erp.controller.admin.asset.vo.assetinput.ErpAssetInputPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetinput.ErpAssetInputRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetinput.ErpAssetInputSaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetInputDO;
import com.kyx.service.erp.service.asset.ErpAssetInputService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 资产录入申请")
@RestController
@RequestMapping("/erp/asset-input")
@Validated
public class ErpAssetInputController {

    @Resource
    private ErpAssetInputService assetInputService;

    @PostMapping("/create")
    @Operation(summary = "创建资产录入申请")
    @PreAuthorize("@ss.hasPermission('erp:asset-input:create')")
    public CommonResult<Long> createAssetInput(@Valid @RequestBody ErpAssetInputSaveReqVO createReqVO) {
        return success(assetInputService.createAssetInput(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建资产录入申请并提交审批")
    @PreAuthorize("@ss.hasPermission('erp:asset-input:create')")
    public CommonResult<Long> createAssetInputAndSubmit(@Valid @RequestBody ErpAssetInputSaveReqVO createReqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return success(assetInputService.createAssetInputAndSubmit(userId, createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产录入申请")
    @PreAuthorize("@ss.hasPermission('erp:asset-input:update')")
    public CommonResult<Boolean> updateAssetInput(@Valid @RequestBody ErpAssetInputSaveReqVO updateReqVO) {
        assetInputService.updateAssetInput(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产录入申请")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:asset-input:delete')")
    public CommonResult<Boolean> deleteAssetInput(@RequestParam("id") Long id) {
        assetInputService.deleteAssetInput(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产录入申请")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-input:query')")
    public CommonResult<ErpAssetInputRespVO> getAssetInput(@RequestParam("id") Long id) {
        ErpAssetInputDO assetInput = assetInputService.getAssetInput(id);
        return success(BeanUtils.toBean(assetInput, ErpAssetInputRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得资产录入申请分页")
    @PreAuthorize("@ss.hasPermission('erp:asset-input:query')")
    public CommonResult<PageResult<ErpAssetInputRespVO>> getAssetInputPage(@Valid ErpAssetInputPageReqVO pageReqVO) {
        return success(assetInputService.getAssetInputVOPage(pageReqVO));
    }

} 