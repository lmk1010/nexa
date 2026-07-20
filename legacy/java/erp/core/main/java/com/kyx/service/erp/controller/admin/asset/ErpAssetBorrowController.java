package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.erp.api.asset.vo.borrow.ErpAssetBorrowSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.borrow.ErpAssetBorrowPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.borrow.ErpAssetBorrowRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetBorrowDO;
import com.kyx.service.erp.service.asset.ErpAssetBorrowService;
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
import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;

/**
 * ERP 资产借用 Controller
 *
 * @author kyx
 */
@Tag(name = "管理后台 - ERP 资产借用")
@RestController
@RequestMapping("/erp/asset-borrow")
@Validated
public class ErpAssetBorrowController {

    @Resource
    private ErpAssetBorrowService borrowService;

    @PostMapping("/create")
    @Operation(summary = "创建资产借用记录")
    @PreAuthorize("@ss.hasPermission('erp:asset-borrow:create')")
    public CommonResult<Long> createBorrow(@Valid @RequestBody ErpAssetBorrowSaveReqVO createReqVO) {
        return success(borrowService.createBorrow(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交资产借用记录（发起BPM流程）")
    @PreAuthorize("@ss.hasPermission('erp:asset-borrow:create')")
    public CommonResult<Long> createBorrowAndSubmit(@Valid @RequestBody ErpAssetBorrowSaveReqVO createReqVO) {
        // 使用当前登录用户ID，这里先硬编码，实际项目中应该从安全上下文获取
        return success(borrowService.createBorrowAndSubmit(1L, createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产借用记录")
    @PreAuthorize("@ss.hasPermission('erp:asset-borrow:update')")
    public CommonResult<Boolean> updateBorrow(@Valid @RequestBody ErpAssetBorrowSaveReqVO updateReqVO) {
        borrowService.updateBorrow(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产借用记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:asset-borrow:delete')")
    public CommonResult<Boolean> deleteBorrow(@RequestParam("id") Long id) {
        borrowService.deleteBorrow(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产借用记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-borrow:query')")
    public CommonResult<ErpAssetBorrowRespVO> getBorrow(@RequestParam("id") String idStr) {
        // 参数验证和转换
        Long id;
        try {
            // 处理前端传递的无效参数
            if ("NaN".equals(idStr) || "null".equals(idStr) || "undefined".equals(idStr)) {
                return CommonResult.error(400, "借用记录ID参数无效，请检查BMP流程中的businessKey配置");
            }
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return CommonResult.error(400, "借用记录ID格式错误: " + idStr);
        }
        
        ErpAssetBorrowDO borrow = borrowService.getBorrow(id);
        return success(BeanUtils.toBean(borrow, ErpAssetBorrowRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得资产借用记录分页")
    @PreAuthorize("@ss.hasPermission('erp:asset-borrow:query')")
    public CommonResult<PageResult<ErpAssetBorrowRespVO>> getBorrowPage(@Valid ErpAssetBorrowPageReqVO pageReqVO) {
        PageResult<ErpAssetBorrowRespVO> pageResult = borrowService.getBorrowPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出资产借用记录 Excel")
    @PreAuthorize("@ss.hasPermission('erp:asset-borrow:export')")
    public void exportBorrowExcel(@Valid ErpAssetBorrowPageReqVO exportReqVO,
                                 HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(Integer.MAX_VALUE);
        List<ErpAssetBorrowRespVO> list = borrowService.getBorrowList(exportReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "资产借用记录.xls", "数据", ErpAssetBorrowRespVO.class, list);
    }

    @PostMapping("/return")
    @Operation(summary = "资产归还")
    @PreAuthorize("@ss.hasPermission('erp:asset-borrow:return')")
    public CommonResult<Boolean> returnAsset(@RequestParam("id") Long id,
                                           @RequestParam("returnCondition") Integer returnCondition,
                                           @RequestParam(value = "returnRemark", required = false) String returnRemark) {
        borrowService.returnAsset(id, returnCondition, returnRemark);
        return success(true);
    }

    @GetMapping("/check-can-borrow")
    @Operation(summary = "检查资产是否可以借用")
    @Parameter(name = "assetId", description = "资产编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:asset-borrow:query')")
    public CommonResult<Boolean> checkCanBorrow(@RequestParam("assetId") Long assetId) {
        boolean canBorrow = borrowService.canBorrow(assetId);
        return success(canBorrow);
    }

    @GetMapping("/overdue")
    @Operation(summary = "获取逾期未还的借用记录")
    @PreAuthorize("@ss.hasPermission('erp:asset-borrow:query')")
    public CommonResult<List<ErpAssetBorrowRespVO>> getOverdueBorrows() {
        List<ErpAssetBorrowDO> list = borrowService.getOverdueBorrows();
        List<ErpAssetBorrowRespVO> respList = BeanUtils.toBean(list, ErpAssetBorrowRespVO.class);
        return success(respList);
    }

    @PostMapping("/update-overdue-status")
    @Operation(summary = "更新逾期状态")
    @PreAuthorize("@ss.hasPermission('erp:asset-borrow:update')")
    public CommonResult<Boolean> updateOverdueStatus(@RequestParam("id") Long id) {
        borrowService.updateOverdueStatus(id);
        return success(true);
    }

    @GetMapping("/get-by-bmp-process-instance-id")
    @Operation(summary = "根据BMP流程实例ID获得资产借用记录")
    @Parameter(name = "bmpProcessInstanceId", description = "BMP流程实例ID", required = true)
    @PreAuthorize("@ss.hasPermission('erp:asset-borrow:query')")
    public CommonResult<ErpAssetBorrowRespVO> getBorrowByBmpProcessInstanceId(@RequestParam("bmpProcessInstanceId") String bmpProcessInstanceId) {
        ErpAssetBorrowDO borrow = borrowService.getBorrowByBmpProcessInstanceId(bmpProcessInstanceId);
        if (borrow == null) {
            return CommonResult.error(404, "未找到对应的资产借用记录，BMP流程实例ID: " + bmpProcessInstanceId);
        }
        return success(BeanUtils.toBean(borrow, ErpAssetBorrowRespVO.class));
    }
} 