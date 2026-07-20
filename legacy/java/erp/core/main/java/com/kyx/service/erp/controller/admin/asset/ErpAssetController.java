package com.kyx.service.erp.controller.admin.asset;

import com.google.common.collect.Lists;
import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetBatchSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetBatchSaveRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetImportExcelVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetImportRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetLogRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCategoryDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetAttachmentDO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpSupplierDO;
import com.kyx.service.erp.service.asset.ErpAssetService;
import com.kyx.service.erp.service.asset.ErpAssetCategoryService;
import com.kyx.service.erp.service.asset.ErpAssetAttachmentService;
import com.kyx.service.erp.service.asset.ErpAssetInputService;
import com.kyx.service.erp.service.purchase.ErpSupplierService;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertList;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.erp.controller.admin.asset.vo.assetinput.ErpAssetInputSaveReqVO;

@Tag(name = "管理后台 - ERP 资产")
@RestController
@RequestMapping("/erp/asset")
@Validated
public class ErpAssetController {

    @Resource
    private ErpAssetService assetService;
    @Resource
    private ErpAssetInputService assetInputService;
    @Resource
    private ErpAssetCategoryService assetCategoryService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpAssetAttachmentService assetAttachmentService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建资产")
    @PreAuthorize("@ss.hasPermission('erp:assets:create')")
    public CommonResult<Long> createAsset(@Valid @RequestBody ErpAssetSaveReqVO createReqVO) {
        return success(assetService.createAsset(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建资产并提交审批")
    @PreAuthorize("@ss.hasPermission('erp:assets:create')")
    public CommonResult<Long> createAssetAndSubmit(@Valid @RequestBody ErpAssetInputSaveReqVO createReqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return success(assetInputService.createAssetInputAndSubmit(userId, createReqVO));
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建资产")
    @PreAuthorize("@ss.hasPermission('erp:assets:create')")
    public CommonResult<ErpAssetBatchSaveRespVO> batchCreateAssets(@Valid @RequestBody ErpAssetBatchSaveReqVO batchSaveReqVO) {
        return success(assetService.batchCreateAssets(batchSaveReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产")
    @PreAuthorize("@ss.hasPermission('erp:assets:update')")
    public CommonResult<Boolean> updateAsset(@Valid @RequestBody ErpAssetSaveReqVO updateReqVO) {
        System.out.println("=== 资产更新接口接收到的数据 ===");
        System.out.println("资产ID: " + updateReqVO.getId());
        System.out.println("资产名称: " + updateReqVO.getName());
        System.out.println("fileIds: " + updateReqVO.getFileIds());
        System.out.println("fileIds大小: " + (updateReqVO.getFileIds() != null ? updateReqVO.getFileIds().size() : "null"));
        System.out.println("================================");

        assetService.updateAsset(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:assets:delete')")
    public CommonResult<Boolean> deleteAsset(@RequestParam("id") Long id) {
        assetService.deleteAsset(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<ErpAssetRespVO> getAsset(@RequestParam("id") Long id) {
        ErpAssetDO asset = assetService.getAsset(id);
        if (asset == null) {
            return success(null);
        }

        // 转换为响应VO
        ErpAssetRespVO respVO = BeanUtils.toBean(asset, ErpAssetRespVO.class);

        // 翻译分类名称
        if (asset.getCategoryId() != null) {
            ErpAssetCategoryDO category = assetCategoryService.getAssetCategory(asset.getCategoryId());
            if (category != null) {
                respVO.setCategoryName(category.getName());
            }
        }

        // 翻译部门名称
        if (asset.getDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(asset.getDeptId()).getCheckedData();
            if (dept != null) {
                respVO.setDeptName(dept.getName());
            }
        }

        // 翻译供应商名称
        if (asset.getSupplierId() != null) {
            ErpSupplierDO supplier = supplierService.getSupplier(asset.getSupplierId());
            if (supplier != null) {
                respVO.setSupplierName(supplier.getName());
            }
        }

        // 加载附件列表
        List<ErpAssetAttachmentDO> attachments = assetAttachmentService.getAssetAttachmentListByAssetId(asset.getId());
        respVO.setAttachments(BeanUtils.toBean(attachments, ErpAssetAttachmentRespVO.class));

        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得资产分页")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<PageResult<ErpAssetRespVO>> getAssetPage(@Valid ErpAssetPageReqVO pageReqVO) {
        return success(assetService.getAssetVOPage(pageReqVO));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得资产精简列表", description = "只包含被开启的资产，主要用于前端的下拉选项")
    public CommonResult<List<ErpAssetRespVO>> getAssetSimpleList() {
        List<ErpAssetRespVO> list = assetService.getAssetVOListByStatus(1);
        return success(list);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出资产 Excel")
    @PreAuthorize("@ss.hasPermission('erp:assets:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAssetExcel(@Valid ErpAssetPageReqVO pageReqVO,
                                 HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpAssetRespVO> list = assetService.getAssetVOPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "资产.xls", "数据", ErpAssetRespVO.class,
                convertList(list, asset -> asset));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得资产导入模板")
    @PreAuthorize("@ss.hasPermission('erp:assets:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        List<ErpAssetImportExcelVO> list = Lists.newArrayList();
        ExcelUtils.write(response, "资产导入模板.xls", "资产列表", ErpAssetImportExcelVO.class, list);
    }

    @GetMapping("/logs/{id}")
    @Operation(summary = "获取资产流转记录")
    @Parameter(name = "id", description = "资产编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<List<ErpAssetLogRespVO>> getAssetLogs(@PathVariable("id") Long id) {
        List<ErpAssetLogRespVO> logs = assetService.getAssetLogs(id);
        return success(logs);
    }

    @PostMapping("/import")
    @Operation(summary = "批量导入资产")
    @Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否支持更新，默认为 false", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('erp:assets:import')")
    public CommonResult<ErpAssetImportRespVO> importAssetExcel(@RequestParam("file") MultipartFile file,
                                                               @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<ErpAssetImportExcelVO> list = ExcelUtils.read(file, ErpAssetImportExcelVO.class);
//        ErpAssetImportRespVO result = assetService.importAssetList(list, updateSupport);
        return success(null);
    }

}
