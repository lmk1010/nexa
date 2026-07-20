package com.kyx.service.erp.controller.admin.asset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.erp.controller.admin.asset.vo.type.ErpAssetTypePageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.type.ErpAssetTypeRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.type.ErpAssetTypeSaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetTypeDO;
import com.kyx.service.erp.service.asset.ErpAssetTypeService;
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
import java.util.List;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 资产类型")
@RestController
@RequestMapping("/erp/asset-type")
@Validated
public class ErpAssetTypeController {

    @Resource
    private ErpAssetTypeService assetTypeService;

    @PostMapping("/create")
    @Operation(summary = "创建资产类型")
    @PreAuthorize("@ss.hasPermission('erp:asset-type:create')")
    public CommonResult<Long> createAssetType(@Valid @RequestBody ErpAssetTypeSaveReqVO createReqVO) {
        return success(assetTypeService.createAssetType(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产类型")
    @PreAuthorize("@ss.hasPermission('erp:asset-type:update')")
    public CommonResult<Boolean> updateAssetType(@Valid @RequestBody ErpAssetTypeSaveReqVO updateReqVO) {
        assetTypeService.updateAssetType(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产类型")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:asset-type:delete')")
    public CommonResult<Boolean> deleteAssetType(@RequestParam("id") Long id) {
        assetTypeService.deleteAssetType(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产类型")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-type:query')")
    public CommonResult<ErpAssetTypeRespVO> getAssetType(@RequestParam("id") Long id) {
        ErpAssetTypeDO assetType = assetTypeService.getAssetType(id);
        return success(BeanUtils.toBean(assetType, ErpAssetTypeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得资产类型分页")
    @PreAuthorize("@ss.hasPermission('erp:asset-type:query')")
    public CommonResult<PageResult<ErpAssetTypeRespVO>> getAssetTypePage(@Valid ErpAssetTypePageReqVO pageReqVO) {
        return success(assetTypeService.getAssetTypeVOPage(pageReqVO));
    }

    @GetMapping("/page-ipage")
    @Operation(summary = "获得资产类型分页（IPage真分页）")
    @PreAuthorize("@ss.hasPermission('erp:asset-type:query')")
    public CommonResult<IPage<ErpAssetTypeRespVO>> getAssetTypePageWithIPage(@Valid ErpAssetTypePageReqVO pageReqVO) {
        return success(assetTypeService.getAssetTypeVOPageWithIPage(pageReqVO));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得资产类型精简列表", description = "只包含被开启的资产类型，主要用于前端的下拉选项")
    public CommonResult<List<ErpAssetTypeRespVO>> getAssetTypeSimpleList() {
        List<ErpAssetTypeDO> list = assetTypeService.getAssetTypeListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(BeanUtils.toBean(list, ErpAssetTypeRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得资产类型列表")
    @PreAuthorize("@ss.hasPermission('erp:asset-type:query')")
    public CommonResult<List<ErpAssetTypeRespVO>> getAssetTypeList() {
        return success(assetTypeService.getAssetTypeVOList());
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出资产类型 Excel")
    @PreAuthorize("@ss.hasPermission('erp:asset-type:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAssetTypeExcel(@Valid ErpAssetTypePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(10000);
        List<ErpAssetTypeRespVO> list = assetTypeService.getAssetTypeVOPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "资产类型.xls", "数据", ErpAssetTypeRespVO.class, list);
    }

} 