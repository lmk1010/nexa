package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.erp.api.asset.vo.lost.ErpAssetLostSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.lost.ErpAssetLostPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.lost.ErpAssetLostRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetLostDO;
import com.kyx.service.erp.service.asset.ErpAssetLostService;
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

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 资产挂失")
@RestController
@RequestMapping("/erp/asset-lost")
@Validated
public class ErpAssetLostController {

    @Resource
    private ErpAssetLostService lostService;

    @PostMapping("/create")
    @Operation(summary = "创建资产挂失记录")
    @PreAuthorize("@ss.hasPermission('eam:asset-lost:create')")
    public CommonResult<Long> createLost(@Valid @RequestBody ErpAssetLostSaveReqVO createReqVO) {
        return success(lostService.createLost(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交资产挂失记录（发起BPM流程）")
    @PreAuthorize("@ss.hasPermission('eam:asset-lost:create')")
    public CommonResult<Long> createLostAndSubmit(@Valid @RequestBody ErpAssetLostSaveReqVO createReqVO) {
        // 使用当前登录用户ID，这里先硬编码，实际项目中应该从安全上下文获取
        return success(lostService.createLostAndSubmit(1L, createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产挂失记录")
    @PreAuthorize("@ss.hasPermission('eam:asset-lost:update')")
    public CommonResult<Boolean> updateLost(@Valid @RequestBody ErpAssetLostSaveReqVO updateReqVO) {
        lostService.updateLost(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产挂失记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('eam:asset-lost:delete')")
    public CommonResult<Boolean> deleteLost(@RequestParam("id") Long id) {
        lostService.deleteLost(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产挂失记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('eam:asset-lost:query')")
    public CommonResult<ErpAssetLostRespVO> getLost(@RequestParam("id") String idStr) {
        Long id = Long.parseLong(idStr);
        ErpAssetLostRespVO lostDetail = lostService.getLostDetail(id);
        return success(lostDetail);
    }

    @GetMapping("/page")
    @Operation(summary = "获得资产挂失记录分页")
    @PreAuthorize("@ss.hasPermission('eam:asset-lost:query')")
    public CommonResult<PageResult<ErpAssetLostRespVO>> getLostPage(@Valid ErpAssetLostPageReqVO pageReqVO) {
        PageResult<ErpAssetLostRespVO> pageResult = lostService.getLostPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出资产挂失记录 Excel")
    @PreAuthorize("@ss.hasPermission('eam:asset-lost:export')")
    public void exportLostExcel(@Valid ErpAssetLostPageReqVO exportReqVO,
                                HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(-1); // 不分页，导出所有数据
        List<ErpAssetLostRespVO> list = lostService.getLostList(exportReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "资产挂失记录.xls", "数据", ErpAssetLostRespVO.class, list);
    }

    // ========== BPM 相关接口 ==========

    @GetMapping("/get-by-bmp-process-instance-id")
    @Operation(summary = "通过 BMP 流程实例编号获取挂失记录")
    @Parameter(name = "bmpProcessInstanceId", description = "BMP流程实例编号", required = true)
    @PreAuthorize("@ss.hasPermission('eam:asset-lost:query')")
    public CommonResult<ErpAssetLostRespVO> getLostByBmpProcessInstanceId(@RequestParam("bmpProcessInstanceId") String bmpProcessInstanceId) {
        ErpAssetLostRespVO lostDetail = lostService.getLostDetailByBmpProcessInstanceId(bmpProcessInstanceId);
        return success(lostDetail);
    }

    @PutMapping("/handle-bmp-status")
    @Operation(summary = "处理BMP状态变更")
    @PreAuthorize("@ss.hasPermission('eam:asset-lost:update')")
    public CommonResult<Boolean> handleBmpStatusChange(@RequestParam("bmpProcessInstanceId") String bmpProcessInstanceId,
                                                       @RequestParam("bmpStatus") Integer bmpStatus) {
        lostService.handleBmpStatusChange(bmpProcessInstanceId, bmpStatus);
        return success(true);
    }

    // ========== 业务处理接口 ==========

    @PutMapping("/handle-found")
    @Operation(summary = "处理找回资产")
    @PreAuthorize("@ss.hasPermission('eam:asset-lost:update')")
    public CommonResult<Boolean> handleFoundAsset(@RequestParam("id") Long id,
                                                  @Valid @RequestBody ErpAssetLostSaveReqVO saveReqVO) {
        lostService.handleFoundAsset(id, saveReqVO);
        return success(true);
    }

    @PutMapping("/confirm-lost")
    @Operation(summary = "确认资产丢失")
    @PreAuthorize("@ss.hasPermission('eam:asset-lost:update')")
    public CommonResult<Boolean> confirmLostAsset(@RequestParam("id") Long id,
                                                  @RequestParam("remark") String remark) {
        lostService.confirmLostAsset(id, remark);
        return success(true);
    }
} 