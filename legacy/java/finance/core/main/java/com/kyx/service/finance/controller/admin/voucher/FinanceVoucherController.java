package com.kyx.service.finance.controller.admin.voucher;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherDetailRespVO;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherPageReqVO;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherRespVO;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherSaveReqVO;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherUpdateReqVO;
import com.kyx.service.finance.service.voucher.FinanceVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 凭证 Controller
 *
 * @author xyang
 */
@RestController
@RequestMapping("/finance/voucher")
@Tag(name = "财务管理 - 凭证")
@Validated
public class FinanceVoucherController {

    @Resource
    private FinanceVoucherService financeVoucherService;

    @Operation(summary = "新增凭证")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('finance:voucher:create')")
    public CommonResult<Long> createVoucher(@Valid @RequestBody FinanceVoucherSaveReqVO reqVO) {
        return success(financeVoucherService.createVoucher(reqVO));
    }

    @Operation(summary = "更新凭证")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('finance:voucher:update')")
    public CommonResult<Boolean> updateVoucher(@Valid @RequestBody FinanceVoucherUpdateReqVO reqVO) {
        return success(financeVoucherService.updateVoucher(reqVO));
    }

    @Operation(summary = "删除凭证")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('finance:voucher:delete')")
    public CommonResult<Boolean> deleteVoucher(@RequestParam Long id) {
        return success(financeVoucherService.deleteVoucher(id));
    }

    @Operation(summary = "作废凭证")
    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('finance:voucher:void')")
    public CommonResult<Boolean> voidVoucher(@RequestParam Long id) {
        return success(financeVoucherService.voidVoucher(id));
    }

    @Operation(summary = "过账凭证")
    @PostMapping("/post")
    @PreAuthorize("@ss.hasPermission('finance:voucher:post')")
    public CommonResult<Boolean> postVoucher(@RequestParam Long id) {
        return success(financeVoucherService.postVoucher(id));
    }

    @Operation(summary = "获取凭证详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasAnyPermissions('finance:voucher:query,finance:voucher:update')")
    public CommonResult<FinanceVoucherRespVO> getVoucher(@RequestParam Long id) {
        FinanceVoucherRespVO respVO = BeanUtils.toBean(financeVoucherService.getVoucher(id), FinanceVoucherRespVO.class);
        respVO.setDetails(BeanUtils.toBean(financeVoucherService.getVoucherDetails(id), FinanceVoucherDetailRespVO.class));
        return success(respVO);
    }

    @Operation(summary = "分页查询凭证")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('finance:voucher:list')")
    public CommonResult<PageResult<FinanceVoucherRespVO>> pageVoucher(@Valid FinanceVoucherPageReqVO reqVO) {
        PageResult<FinanceVoucherRespVO> pageResult =
                BeanUtils.toBean(financeVoucherService.pageVoucher(reqVO), FinanceVoucherRespVO.class);
        return success(pageResult);
    }
}
