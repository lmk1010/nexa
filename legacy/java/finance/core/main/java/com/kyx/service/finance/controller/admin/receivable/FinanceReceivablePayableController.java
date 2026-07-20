package com.kyx.service.finance.controller.admin.receivable;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayablePageReqVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableRespVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableSaveReqVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableUpdateReqVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableWriteOffPageReqVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableWriteOffRespVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableWriteOffSaveReqVO;
import com.kyx.service.finance.service.receivable.FinanceReceivablePayableService;
import com.kyx.service.finance.service.support.FinanceExecutionGuardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 往来账 Controller
 *
 * @author xyang
 */
@RestController
@RequestMapping({"/finance/receivable-payable", "/finance/settlement/arp"})
@Tag(name = "财务管理 - 往来账")
@Validated
public class FinanceReceivablePayableController {

    private static final long IDEMPOTENT_TIMEOUT_SECONDS = 10L;

    @Resource
    private FinanceReceivablePayableService financeReceivablePayableService;
    @Resource
    private FinanceExecutionGuardService financeExecutionGuardService;

    @Operation(summary = "新增往来账")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('finance:receivable-payable:create')")
    public CommonResult<Long> createReceivablePayable(@Valid @RequestBody FinanceReceivablePayableSaveReqVO reqVO) {
        return success(financeReceivablePayableService.createReceivablePayable(reqVO));
    }

    @Operation(summary = "更新往来账")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('finance:receivable-payable:update')")
    public CommonResult<Boolean> updateReceivablePayable(@Valid @RequestBody FinanceReceivablePayableUpdateReqVO reqVO) {
        return success(financeReceivablePayableService.updateReceivablePayable(reqVO));
    }

    @Operation(summary = "删除往来账")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('finance:receivable-payable:delete')")
    public CommonResult<Boolean> deleteReceivablePayable(@RequestParam Long id) {
        return success(financeReceivablePayableService.deleteReceivablePayable(id));
    }

    @Operation(summary = "获取往来账详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasAnyPermissions('finance:receivable-payable:query,finance:receivable-payable:update')")
    public CommonResult<FinanceReceivablePayableRespVO> getReceivablePayable(@RequestParam Long id) {
        return success(BeanUtils.toBean(financeReceivablePayableService.getReceivablePayable(id), FinanceReceivablePayableRespVO.class));
    }

    @Operation(summary = "分页查询往来账")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('finance:receivable-payable:list')")
    public CommonResult<PageResult<FinanceReceivablePayableRespVO>> pageReceivablePayable(@Valid FinanceReceivablePayablePageReqVO reqVO) {
        return success(BeanUtils.toBean(financeReceivablePayableService.pageReceivablePayable(reqVO), FinanceReceivablePayableRespVO.class));
    }

    @Operation(summary = "新增往来账核销")
    @PostMapping({"/write-off/create", "/writeoff/create"})
    @PreAuthorize("@ss.hasAnyPermissions('finance:receivable-payable:write-off:create,finance:receivable-payable:writeoff')")
    public CommonResult<Long> createWriteOff(@Valid @RequestBody FinanceReceivablePayableWriteOffSaveReqVO reqVO) {
        String bizKey = buildWriteOffBizKey(reqVO);
        Long id = financeExecutionGuardService.executeWithIdempotentAndRetry(
                "receivable:writeoff:create", bizKey, IDEMPOTENT_TIMEOUT_SECONDS,
                () -> financeReceivablePayableService.createWriteOff(reqVO));
        return success(id);
    }

    @Operation(summary = "分页查询往来账核销记录")
    @GetMapping({"/write-off/page", "/writeoff/page"})
    @PreAuthorize("@ss.hasAnyPermissions('finance:receivable-payable:write-off:list,finance:receivable-payable:writeoff')")
    public CommonResult<PageResult<FinanceReceivablePayableWriteOffRespVO>> pageWriteOff(@Valid FinanceReceivablePayableWriteOffPageReqVO reqVO) {
        return success(BeanUtils.toBean(financeReceivablePayableService.pageWriteOff(reqVO), FinanceReceivablePayableWriteOffRespVO.class));
    }

    private String buildWriteOffBizKey(FinanceReceivablePayableWriteOffSaveReqVO reqVO) {
        StringBuilder builder = new StringBuilder();
        builder.append(reqVO.getArpId()).append(":")
                .append(reqVO.getAmount()).append(":")
                .append(reqVO.getWriteOffDate());
        if (reqVO.getTransactionId() != null) {
            builder.append(":txn=").append(reqVO.getTransactionId());
        }
        if (reqVO.getAccountId() != null) {
            builder.append(":acc=").append(reqVO.getAccountId());
        }
        if (StringUtils.hasText(reqVO.getTransactionNo())) {
            builder.append(":no=").append(reqVO.getTransactionNo().trim());
        }
        return builder.toString();
    }
}
