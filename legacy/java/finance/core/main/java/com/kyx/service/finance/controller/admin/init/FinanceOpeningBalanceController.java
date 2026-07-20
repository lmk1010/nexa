package com.kyx.service.finance.controller.admin.init;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceBatchSaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceListReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceLockReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalancePageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceRespVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceRollReqVO;
import com.kyx.service.finance.service.init.FinanceOpeningBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 期初余额控制器
 *
 * @author xyang
 */
@RestController
@RequestMapping("/finance/init/opening-balance")
@Tag(name = "财务初始化 - 期初余额")
@Validated
public class FinanceOpeningBalanceController {

    @Resource
    private FinanceOpeningBalanceService financeOpeningBalanceService;

    @Operation(summary = "批量保存期初余额")
    @PostMapping("/batch-save")
    @PreAuthorize("@ss.hasPermission('finance:opening-balance:save')")
    public CommonResult<Boolean> batchSaveOpeningBalance(@Valid @RequestBody FinanceOpeningBalanceBatchSaveReqVO reqVO) {
        return success(financeOpeningBalanceService.batchSaveOpeningBalance(reqVO));
    }

    @Operation(summary = "锁定/解锁期初余额")
    @PostMapping("/lock")
    @PreAuthorize("@ss.hasPermission('finance:opening-balance:lock')")
    public CommonResult<Boolean> lockOpeningBalance(@Valid @RequestBody FinanceOpeningBalanceLockReqVO reqVO) {
        return success(financeOpeningBalanceService.lockOpeningBalance(reqVO));
    }

    @Operation(summary = "解锁期初余额（规划路径）")
    @PostMapping("/unlock")
    @PreAuthorize("@ss.hasAnyPermissions('finance:opening-balance:unlock,finance:opening-balance:lock')")
    public CommonResult<Boolean> unlockOpeningBalance(@Valid @RequestBody FinanceOpeningBalanceLockReqVO reqVO) {
        reqVO.setLocked(Boolean.FALSE);
        return success(financeOpeningBalanceService.lockOpeningBalance(reqVO));
    }

    @Operation(summary = "滚动期初余额到目标期间")
    @PostMapping("/roll")
    @PreAuthorize("@ss.hasAnyPermissions('finance:opening-balance:roll,finance:opening-balance:save')")
    public CommonResult<Boolean> rollOpeningBalance(@Valid @RequestBody FinanceOpeningBalanceRollReqVO reqVO) {
        return success(financeOpeningBalanceService.rollOpeningBalance(reqVO));
    }

    @Operation(summary = "分页查询期初余额")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('finance:opening-balance:list')")
    public CommonResult<PageResult<FinanceOpeningBalanceRespVO>> pageOpeningBalance(@Valid FinanceOpeningBalancePageReqVO reqVO) {
        return success(BeanUtils.toBean(financeOpeningBalanceService.pageOpeningBalance(reqVO), FinanceOpeningBalanceRespVO.class));
    }

    @Operation(summary = "列表查询期初余额（不分页）")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('finance:opening-balance:list')")
    public CommonResult<List<FinanceOpeningBalanceRespVO>> listOpeningBalance(@Valid FinanceOpeningBalanceListReqVO reqVO) {
        return success(BeanUtils.toBean(financeOpeningBalanceService.listOpeningBalance(reqVO), FinanceOpeningBalanceRespVO.class));
    }

}
