package com.kyx.service.erp.controller.admin.asset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.erp.controller.admin.asset.vo.category.ErpAssetCategoryPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.category.ErpAssetCategoryRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.category.ErpAssetCategorySaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCategoryDO;
import com.kyx.service.erp.service.asset.ErpAssetCategoryService;
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
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 资产分类")
@RestController
@RequestMapping("/erp/asset-category")
@Validated
public class ErpAssetCategoryController {

    @Resource
    private ErpAssetCategoryService assetCategoryService;

    @PostMapping("/create")
    @Operation(summary = "创建资产分类")
    @PreAuthorize("@ss.hasPermission('erp:assets-category:create')")
    public CommonResult<Long> createAssetCategory(@Valid @RequestBody ErpAssetCategorySaveReqVO createReqVO) {
        return success(assetCategoryService.createAssetCategory(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产分类")
    @PreAuthorize("@ss.hasPermission('erp:assets-category:update')")
    public CommonResult<Boolean> updateAssetCategory(@Valid @RequestBody ErpAssetCategorySaveReqVO updateReqVO) {
        assetCategoryService.updateAssetCategory(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产分类")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:assets-category:delete')")
    public CommonResult<Boolean> deleteAssetCategory(@RequestParam("id") Long id) {
        assetCategoryService.deleteAssetCategory(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产分类")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:assets-category:query')")
    public CommonResult<ErpAssetCategoryRespVO> getAssetCategory(@RequestParam("id") Long id) {
        ErpAssetCategoryDO assetCategory = assetCategoryService.getAssetCategory(id);
        return success(BeanUtils.toBean(assetCategory, ErpAssetCategoryRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得资产分类分页")
    @PreAuthorize("@ss.hasPermission('erp:assets-category:query')")
    public CommonResult<PageResult<ErpAssetCategoryRespVO>> getAssetCategoryPage(@Valid ErpAssetCategoryPageReqVO pageReqVO) {
        return success(assetCategoryService.getAssetCategoryVOPage(pageReqVO));
    }

    @GetMapping("/page-ipage")
    @Operation(summary = "获得资产分类分页（IPage真分页）")
    @PreAuthorize("@ss.hasPermission('erp:assets-category:query')")
    public CommonResult<IPage<ErpAssetCategoryRespVO>> getAssetCategoryPageWithIPage(@Valid ErpAssetCategoryPageReqVO pageReqVO) {
        return success(assetCategoryService.getAssetCategoryVOPageWithIPage(pageReqVO));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得资产分类精简列表", description = "只包含被开启的资产分类，主要用于前端的下拉选项")
    @PreAuthorize("@ss.hasPermission('erp:assets-category:list')")
    public CommonResult<List<ErpAssetCategoryRespVO>> getAssetCategorySimpleList() {
        List<ErpAssetCategoryDO> list = assetCategoryService.getAssetCategoryListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(BeanUtils.toBean(list, ErpAssetCategoryRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得资产分类列表")
    @PreAuthorize("@ss.hasPermission('erp:assets-category:query')")
    public CommonResult<List<ErpAssetCategoryRespVO>> getAssetCategoryList() {
        return success(assetCategoryService.getAssetCategoryVOList());
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出资产分类 Excel")
    @PreAuthorize("@ss.hasPermission('erp:assets-category:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAssetCategoryExcel(@Valid ErpAssetCategoryPageReqVO pageReqVO,
                                         HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpAssetCategoryRespVO> list = assetCategoryService.getAssetCategoryVOPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "资产分类.xls", "数据", ErpAssetCategoryRespVO.class,
                convertList(list, category -> category));
    }

} 