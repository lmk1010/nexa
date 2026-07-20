package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryPlanPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryPlanRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryPlanSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpAssetInventoryReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpAssetInventoryRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpValidateScanReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpValidateScanRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpConfirmInventoryReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryPlanProgressRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryRecordPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryRecordRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpInventoryPlanDO;
import com.kyx.service.erp.service.asset.ErpInventoryPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
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
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 盘点计划")
@RestController
@RequestMapping("/erp/inventory-plan")
@Validated
public class ErpInventoryPlanController {

    @Resource
    private ErpInventoryPlanService inventoryPlanService;

    @PostMapping("/create")
    @Operation(summary = "创建盘点计划")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:create')")
    public CommonResult<Long> createInventoryPlan(@Valid @RequestBody ErpInventoryPlanSaveReqVO createReqVO) {
        return success(inventoryPlanService.createInventoryPlan(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新盘点计划")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:update')")
    public CommonResult<Boolean> updateInventoryPlan(@Valid @RequestBody ErpInventoryPlanSaveReqVO updateReqVO) {
        inventoryPlanService.updateInventoryPlan(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除盘点计划")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:delete')")
    public CommonResult<Boolean> deleteInventoryPlan(@RequestParam("id") Long id) {
        inventoryPlanService.deleteInventoryPlan(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得盘点计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:query')")
    public CommonResult<ErpInventoryPlanRespVO> getInventoryPlan(@RequestParam("id") Long id) {
        ErpInventoryPlanDO inventoryPlan = inventoryPlanService.getInventoryPlan(id);
        return success(BeanUtils.toBean(inventoryPlan, ErpInventoryPlanRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得盘点计划分页")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:query')")
    public CommonResult<PageResult<ErpInventoryPlanRespVO>> getInventoryPlanPage(@Valid ErpInventoryPlanPageReqVO pageVO) {
        PageResult<ErpInventoryPlanDO> pageResult = inventoryPlanService.getInventoryPlanPage(pageVO);
        return success(BeanUtils.toBean(pageResult, ErpInventoryPlanRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出盘点计划 Excel")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportInventoryPlanExcel(@Valid ErpInventoryPlanPageReqVO pageVO,
                                         HttpServletResponse response) throws IOException {
        pageVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpInventoryPlanDO> list = inventoryPlanService.getInventoryPlanList(pageVO);
        // 导出 Excel
        ExcelUtils.write(response, "盘点计划.xls", "数据", ErpInventoryPlanRespVO.class,
                convertList(list, plan -> BeanUtils.toBean(plan, ErpInventoryPlanRespVO.class)));
    }

    @PostMapping("/submit-approval")
    @Operation(summary = "提交审批")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:submit-approval')")
    public CommonResult<Boolean> submitApproval(@RequestParam("id") Long id) {
        inventoryPlanService.submitApproval(id);
        return success(true);
    }

    @PostMapping("/approve")
    @Operation(summary = "审批盘点计划")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "passed", description = "是否通过", required = true),
            @Parameter(name = "remark", description = "审批备注")
    })
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:approve')")
    public CommonResult<Boolean> approveInventoryPlan(@RequestParam("id") Long id,
                                                      @RequestParam("passed") Boolean passed,
                                                      @RequestParam(value = "remark", required = false) String remark) {
        inventoryPlanService.approveInventoryPlan(id, passed, remark);
        return success(true);
    }

    @PostMapping("/start")
    @Operation(summary = "开始执行盘点计划")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:start')")
    public CommonResult<Boolean> startInventoryPlan(@RequestParam("id") Long id) {
        inventoryPlanService.startInventoryPlan(id);
        return success(true);
    }

    @PostMapping("/complete")
    @Operation(summary = "完成盘点计划")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:complete')")
    public CommonResult<Boolean> completeInventoryPlan(@RequestParam("id") Long id) {
        inventoryPlanService.completeInventoryPlan(id);
        return success(true);
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消盘点计划")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:cancel')")
    public CommonResult<Boolean> cancelInventoryPlan(@RequestParam("id") Long id) {
        inventoryPlanService.cancelInventoryPlan(id);
        return success(true);
    }

    @PostMapping("/update-progress")
    @Operation(summary = "更新盘点进度")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "completedAssetCount", description = "已完成数量", required = true)
    })
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:update-progress')")
    public CommonResult<Boolean> updateInventoryProgress(@RequestParam("id") Long id,
                                                         @RequestParam("completedAssetCount") Integer completedAssetCount) {
        inventoryPlanService.updateInventoryProgress(id, completedAssetCount);
        return success(true);
    }

    @GetMapping("/active-execution-plans")
    @Operation(summary = "获取正在执行的盘点计划列表")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:query')")
    public CommonResult<List<ErpInventoryPlanRespVO>> getActiveExecutionPlans() {
        List<ErpInventoryPlanDO> list = inventoryPlanService.getActiveExecutionPlans();
        return success(convertList(list, plan -> BeanUtils.toBean(plan, ErpInventoryPlanRespVO.class)));
    }

    @GetMapping("/generate-asset-list")
    @Operation(summary = "生成待盘点资产列表")
    @Parameter(name = "planId", description = "计划编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:generate-asset-list')")
    public CommonResult<List<Long>> generateInventoryAssetList(@RequestParam("planId") Long planId) {
        List<Long> assetIds = inventoryPlanService.generateInventoryAssetList(planId);
        return success(assetIds);
    }

    @GetMapping("/list-by-status")
    @Operation(summary = "根据状态获取盘点计划列表")
    @Parameter(name = "status", description = "状态", required = true)
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:query')")
    public CommonResult<List<ErpInventoryPlanRespVO>> getInventoryPlanListByStatus(@RequestParam("status") Integer status) {
        List<ErpInventoryPlanDO> list = inventoryPlanService.getInventoryPlanListByStatus(status);
        return success(convertList(list, plan -> BeanUtils.toBean(plan, ErpInventoryPlanRespVO.class)));
    }

    @GetMapping("/list-by-responsible-person")
    @Operation(summary = "根据负责人获取盘点计划列表")
    @Parameter(name = "responsiblePersonId", description = "负责人ID", required = true)
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:query')")
    public CommonResult<List<ErpInventoryPlanRespVO>> getInventoryPlanListByResponsiblePerson(@RequestParam("responsiblePersonId") Long responsiblePersonId) {
        List<ErpInventoryPlanDO> list = inventoryPlanService.getInventoryPlanListByResponsiblePerson(responsiblePersonId);
        return success(convertList(list, plan -> BeanUtils.toBean(plan, ErpInventoryPlanRespVO.class)));
    }

    @PostMapping("/scan-asset")
    @Operation(summary = "扫描资产进行盘点")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:scan-asset')")
    public CommonResult<ErpAssetInventoryRespVO> scanAssetInventory(@Valid @RequestBody ErpAssetInventoryReqVO reqVO) {
        ErpAssetInventoryRespVO respVO = inventoryPlanService.scanAssetInventory(reqVO);
        return success(respVO);
    }

    @PostMapping("/validate-scan")
    @Operation(summary = "验证扫码内容")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:scan-asset')")
    public CommonResult<ErpValidateScanRespVO> validateScanCode(@Valid @RequestBody ErpValidateScanReqVO reqVO) {
        ErpValidateScanRespVO respVO = inventoryPlanService.validateScanCode(reqVO);
        return success(respVO);
    }

    @PostMapping("/confirm-inventory")
    @Operation(summary = "确认盘点结果")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:scan-asset')")
    public CommonResult<ErpAssetInventoryRespVO> confirmInventory(@Valid @RequestBody ErpConfirmInventoryReqVO reqVO) {
        ErpAssetInventoryRespVO respVO = inventoryPlanService.confirmInventory(reqVO);
        return success(respVO);
    }

    @PostMapping("/publish")
    @Operation(summary = "发布盘点计划")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:publish')")
    public CommonResult<Boolean> publishInventoryPlan(@RequestParam("id") Long id) {
        inventoryPlanService.publishInventoryPlan(id);
        return success(true);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交盘点计划")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:submit')")
    public CommonResult<Boolean> submitInventoryPlan(@RequestParam("id") Long id, 
                                                     @RequestParam("reason") String reason) {
        inventoryPlanService.submitInventoryPlan(id, reason);
        return success(true);
    }

    @PostMapping("/audit")
    @Operation(summary = "审核盘点计划")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "passed", description = "是否通过", required = true),
            @Parameter(name = "remark", description = "审核备注")
    })
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:audit')")
    public CommonResult<Boolean> auditInventoryPlan(@RequestParam("id") Long id,
                                                    @RequestParam("passed") Boolean passed,
                                                    @RequestParam(value = "remark", required = false) String remark) {
        inventoryPlanService.auditInventoryPlan(id, passed, remark);
        return success(true);
    }

    @GetMapping("/progress/{planId}")
    @Operation(summary = "获取盘点执行进度")
    @Parameter(name = "planId", description = "盘点计划编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:query')")
    public CommonResult<ErpInventoryPlanProgressRespVO> getInventoryPlanProgress(@PathVariable("planId") Long planId) {
        ErpInventoryPlanProgressRespVO progress = inventoryPlanService.getInventoryPlanProgress(planId);
        return success(progress);
    }

    @GetMapping("/records/page")
    @Operation(summary = "获得盘点记录分页")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:query')")
    public CommonResult<PageResult<ErpInventoryRecordRespVO>> getInventoryRecordPage(@Valid ErpInventoryRecordPageReqVO pageVO) {
        PageResult<ErpInventoryRecordRespVO> pageResult = inventoryPlanService.getInventoryRecordPage(pageVO);
        return success(pageResult);
    }

    @GetMapping("/records/list/{planId}")
    @Operation(summary = "获取盘点记录列表")
    @Parameter(name = "planId", description = "盘点计划编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:query')")
    public CommonResult<List<ErpInventoryRecordRespVO>> getInventoryRecordList(@PathVariable("planId") Long planId) {
        List<ErpInventoryRecordRespVO> records = inventoryPlanService.getInventoryRecordList(planId);
        return success(records);
    }

    @GetMapping("/records/abnormal/{planId}")
    @Operation(summary = "获取异常盘点记录列表")
    @Parameter(name = "planId", description = "盘点计划编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('erp:inventory-plan:query')")
    public CommonResult<List<ErpInventoryRecordRespVO>> getAbnormalInventoryRecordList(@PathVariable("planId") Long planId) {
        List<ErpInventoryRecordRespVO> records = inventoryPlanService.getAbnormalInventoryRecordList(planId);
        return success(records);
    }

} 