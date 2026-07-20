package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.erp.api.asset.vo.scrapped.ErpAssetScrappedSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.scrapped.ErpAssetScrappedPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.scrapped.ErpAssetScrappedRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetScrappedDO;
import com.kyx.service.erp.service.asset.ErpAssetScrappedService;
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
import java.math.BigDecimal;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * ERP 资产报废 Controller
 *
 * @author kyx
 */
@Tag(name = "管理后台 - ERP 资产报废")
@RestController
@RequestMapping("/erp/asset-scrapped")
@Validated
public class ErpAssetScrappedController {

    @Resource
    private ErpAssetScrappedService scrappedService;

    @PostMapping("/create")
    @Operation(summary = "创建资产报废记录")
    @PreAuthorize("@ss.hasPermission('eam:asset-scrapped:create')")
    public CommonResult<Long> createScrapped(@Valid @RequestBody ErpAssetScrappedSaveReqVO createReqVO) {
        return success(scrappedService.createScrapped(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交资产报废记录（发起BPM流程）")
    @PreAuthorize("@ss.hasPermission('eam:asset-scrapped:create')")
    public CommonResult<Long> createScrappedAndSubmit(@Valid @RequestBody ErpAssetScrappedSaveReqVO createReqVO) {
        // 使用当前登录用户ID，这里先硬编码，实际项目中应该从安全上下文获取
        return success(scrappedService.createScrappedAndSubmit(1L, createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产报废记录")
    @PreAuthorize("@ss.hasPermission('eam:asset-scrapped:update')")
    public CommonResult<Boolean> updateScrapped(@Valid @RequestBody ErpAssetScrappedSaveReqVO updateReqVO) {
        scrappedService.updateScrapped(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产报废记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('eam:asset-scrapped:delete')")
    public CommonResult<Boolean> deleteScrapped(@RequestParam("id") Long id) {
        scrappedService.deleteScrapped(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产报废记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('eam:asset-scrapped:query')")
    public CommonResult<ErpAssetScrappedRespVO> getScrapped(@RequestParam("id") String idStr) {
        // 参数验证和转换
        Long id;
        try {
            // 处理前端传递的无效参数
            if ("NaN".equals(idStr) || "null".equals(idStr) || "undefined".equals(idStr)) {
                return CommonResult.error(400, "报废记录ID参数无效，请检查BMP流程中的businessKey配置");
            }
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return CommonResult.error(400, "报废记录ID格式错误: " + idStr);
        }
        
        ErpAssetScrappedDO scrapped = scrappedService.getScrapped(id);
        return success(BeanUtils.toBean(scrapped, ErpAssetScrappedRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得资产报废记录分页")
    @PreAuthorize("@ss.hasPermission('eam:asset-scrapped:query')")
    public CommonResult<PageResult<ErpAssetScrappedRespVO>> getScrappedPage(@Valid ErpAssetScrappedPageReqVO pageReqVO) {
        PageResult<ErpAssetScrappedRespVO> pageResult = scrappedService.getScrappedPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出资产报废记录 Excel")
    @PreAuthorize("@ss.hasPermission('eam:asset-scrapped:export')")
    public void exportScrappedExcel(@Valid ErpAssetScrappedPageReqVO exportReqVO,
                                   HttpServletResponse response) throws IOException {
        List<ErpAssetScrappedRespVO> list = scrappedService.getScrappedList(exportReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "资产报废记录.xls", "数据", ErpAssetScrappedRespVO.class, list);
    }

    @PostMapping("/complete")
    @Operation(summary = "完成资产报废处理")
    @PreAuthorize("@ss.hasPermission('eam:asset-scrapped:update')")
    public CommonResult<Boolean> completeScrapped(@RequestParam("id") Long id,
                                                 @RequestParam("processingMethod") String processingMethod,
                                                 @RequestParam(value = "disposalRevenue", required = false) BigDecimal disposalRevenue,
                                                 @RequestParam(value = "remark", required = false) String remark) {
        scrappedService.completeScrapped(id, processingMethod, disposalRevenue, remark);
        return success(true);
    }

    @GetMapping("/get-by-bmp-process-instance-id")
    @Operation(summary = "根据BMP流程实例ID获得资产报废记录")
    @Parameter(name = "bmpProcessInstanceId", description = "BMP流程实例ID", required = true)
    @PreAuthorize("@ss.hasPermission('eam:asset-scrapped:query')")
    public CommonResult<ErpAssetScrappedRespVO> getScrappedByBmpProcessInstanceId(@RequestParam("bmpProcessInstanceId") String bmpProcessInstanceId) {
        ErpAssetScrappedDO scrapped = scrappedService.getScrappedByBmpProcessInstanceId(bmpProcessInstanceId);
        if (scrapped == null) {
            return CommonResult.error(404, "未找到对应的资产报废记录，BMP流程实例ID: " + bmpProcessInstanceId);
        }
        return success(BeanUtils.toBean(scrapped, ErpAssetScrappedRespVO.class));
    }
} 