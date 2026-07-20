package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.erp.controller.admin.asset.vo.barcode.*;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetBarcodePrintDO;
import com.kyx.service.erp.service.asset.ErpAssetBarcodePrintService;
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

@Tag(name = "管理后台 - ERP 资产条码打印")
@RestController
@RequestMapping("/erp/asset-barcode-print")
@Validated
public class ErpAssetBarcodePrintController {

    @Resource
    private ErpAssetBarcodePrintService assetBarcodePrintService;

    @PostMapping("/create")
    @Operation(summary = "创建资产条码打印记录")
    @PreAuthorize("@ss.hasPermission('erp:barcode-print:create')")
    public CommonResult<Long> createAssetBarcodePrint(@Valid @RequestBody ErpAssetBarcodePrintSaveReqVO createReqVO) {
        return success(assetBarcodePrintService.createAssetBarcodePrint(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产条码打印记录")
    @PreAuthorize("@ss.hasPermission('erp:barcode-print:update')")
    public CommonResult<Boolean> updateAssetBarcodePrint(@Valid @RequestBody ErpAssetBarcodePrintSaveReqVO updateReqVO) {
        assetBarcodePrintService.updateAssetBarcodePrint(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产条码打印记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:barcode-print:delete')")
    public CommonResult<Boolean> deleteAssetBarcodePrint(@RequestParam("id") Long id) {
        assetBarcodePrintService.deleteAssetBarcodePrint(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产条码打印记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:barcode-print:query')")
    public CommonResult<ErpAssetBarcodePrintDO> getAssetBarcodePrint(@RequestParam("id") Long id) {
        ErpAssetBarcodePrintDO assetBarcodePrint = assetBarcodePrintService.getAssetBarcodePrint(id);
        return success(assetBarcodePrint);
    }

    @GetMapping("/page")
    @Operation(summary = "获得资产条码打印记录分页")
    @PreAuthorize("@ss.hasPermission('erp:barcode-print:query')")
    public CommonResult<PageResult<ErpAssetBarcodePrintRespVO>> getAssetBarcodePrintPage(@Valid ErpAssetBarcodePrintPageReqVO pageReqVO) {
        PageResult<ErpAssetBarcodePrintRespVO> pageResult = assetBarcodePrintService.getAssetBarcodePrintVOPage(pageReqVO);
        return success(pageResult);
    }

    @PostMapping("/generate-barcodes")
    @Operation(summary = "批量生成资产条码")
    @PreAuthorize("@ss.hasPermission('erp:barcode-print:generate')")
    public CommonResult<Integer> generateAssetBarcodes(@Valid @RequestBody ErpAssetBarcodeGenerateReqVO generateReqVO) {
        int count = assetBarcodePrintService.generateAssetBarcodes(generateReqVO);
        return success(count);
    }

    @PostMapping("/print-barcodes")
    @Operation(summary = "批量打印资产条码")
    @PreAuthorize("@ss.hasPermission('erp:barcode-print:print')")
    public CommonResult<Boolean> printAssetBarcodes(@Valid @RequestBody ErpAssetBarcodePrintReqVO printReqVO) {
        boolean result = assetBarcodePrintService.printAssetBarcodes(printReqVO);
        return success(result);
    }

    @GetMapping("/by-asset/{assetId}")
    @Operation(summary = "根据资产编号获取条码打印记录")
    @Parameter(name = "assetId", description = "资产编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('erp:barcode-print:query')")
    public CommonResult<List<ErpAssetBarcodePrintDO>> getAssetBarcodePrintListByAssetId(@PathVariable("assetId") Long assetId) {
        List<ErpAssetBarcodePrintDO> list = assetBarcodePrintService.getAssetBarcodePrintListByAssetId(assetId);
        return success(list);
    }

    @GetMapping("/by-barcode/{barcodeNo}")
    @Operation(summary = "根据条码编号获取条码打印记录")
    @Parameter(name = "barcodeNo", description = "条码编号", required = true, example = "BC001001")
    @PreAuthorize("@ss.hasPermission('erp:barcode-print:query')")
    public CommonResult<ErpAssetBarcodePrintDO> getAssetBarcodePrintByBarcodeNo(@PathVariable("barcodeNo") String barcodeNo) {
        ErpAssetBarcodePrintDO barcodePrint = assetBarcodePrintService.getAssetBarcodePrintByBarcodeNo(barcodeNo);
        return success(barcodePrint);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出资产条码打印记录 Excel")
    @PreAuthorize("@ss.hasPermission('erp:barcode-print:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAssetBarcodePrintExcel(@Valid ErpAssetBarcodePrintPageReqVO pageReqVO,
                                             HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpAssetBarcodePrintRespVO> list = assetBarcodePrintService.getAssetBarcodePrintPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "资产条码打印记录.xls", "数据", ErpAssetBarcodePrintRespVO.class, list);
    }

} 