package com.kyx.service.finance.controller.admin.init;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountBatchDeleteReqVO;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountBatchStatusUpdateReqVO;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountOptionCreateReqVO;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountOptionRespVO;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountOptionUpdateReqVO;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountPageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountRespVO;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountSaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountStatusUpdateReqVO;
import com.kyx.service.finance.service.init.FinanceAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 账户控制器
 *
 * @author xyang
 */
@RestController
@RequestMapping("/finance/init/account")
@Tag(name = "财务初始化 - 账户管理")
@Validated
public class FinanceAccountController {

    @Resource
    private FinanceAccountService financeAccountService;

    @Operation(summary = "创建账户")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('finance:account:create')")
    public CommonResult<Long> createAccount(@Valid @RequestBody FinanceAccountSaveReqVO reqVO) {
        return success(financeAccountService.createAccount(reqVO));
    }

    @Operation(summary = "更新账户")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('finance:account:update')")
    public CommonResult<Boolean> updateAccount(@Valid @RequestBody FinanceAccountSaveReqVO reqVO) {
        return success(financeAccountService.updateAccount(reqVO));
    }

    @Operation(summary = "删除账户")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('finance:account:delete')")
    public CommonResult<Boolean> deleteAccount(@RequestParam Long id) {
        return success(financeAccountService.deleteAccount(id));
    }

    @Operation(summary = "更新账户状态")
    @PutMapping("/update-status")
    @PreAuthorize("@ss.hasPermission('finance:account:update')")
    public CommonResult<Boolean> updateAccountStatus(@Valid @RequestBody FinanceAccountStatusUpdateReqVO reqVO) {
        return success(financeAccountService.updateAccountStatus(reqVO.getId(), reqVO.getStatus()));
    }

    @Operation(summary = "批量更新账户状态")
    @PutMapping("/batch-update-status")
    @PreAuthorize("@ss.hasPermission('finance:account:update')")
    public CommonResult<Boolean> batchUpdateAccountStatus(@Valid @RequestBody FinanceAccountBatchStatusUpdateReqVO reqVO) {
        return success(financeAccountService.batchUpdateAccountStatus(reqVO.getIds(), reqVO.getStatus()));
    }

    @Operation(summary = "批量删除账户")
    @PostMapping("/batch-delete")
    @PreAuthorize("@ss.hasPermission('finance:account:delete')")
    public CommonResult<Boolean> batchDeleteAccount(@Valid @RequestBody FinanceAccountBatchDeleteReqVO reqVO) {
        return success(financeAccountService.batchDeleteAccount(reqVO.getIds()));
    }

    @Operation(summary = "获取账户详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasAnyPermissions('finance:account:query,finance:account:list,finance:transaction:list,finance:transaction:update')")
    public CommonResult<FinanceAccountRespVO> getAccount(@RequestParam Long id) {
        return success(BeanUtils.toBean(financeAccountService.getAccount(id), FinanceAccountRespVO.class));
    }

    @Operation(summary = "分页查询账户")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasAnyPermissions('finance:account:list,finance:transaction:list,finance:receivable-payable:list,finance:receivable-payable:write-off:create,finance:receivable-payable:writeoff')")
    public CommonResult<PageResult<FinanceAccountRespVO>> pageAccount(@Valid FinanceAccountPageReqVO reqVO) {
        return success(BeanUtils.toBean(financeAccountService.pageAccount(reqVO), FinanceAccountRespVO.class));
    }

    @Operation(summary = "查询企业主体选项")
    @GetMapping("/company-entity/simple-list")
    @PreAuthorize("@ss.hasAnyPermissions('finance:account:query,finance:account:list')")
    public CommonResult<List<String>> getCompanyEntityOptions() {
        return success(financeAccountService.getCompanyEntityOptions());
    }

    @Operation(summary = "查询企业主体选项列表")
    @GetMapping("/company-entity/list")
    @PreAuthorize("@ss.hasAnyPermissions('finance:account:query,finance:account:list')")
    public CommonResult<List<FinanceAccountOptionRespVO>> getCompanyEntityOptionList(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(BeanUtils.toBean(financeAccountService.getCompanyEntityOptionList(keyword),
                FinanceAccountOptionRespVO.class));
    }

    @Operation(summary = "新增企业主体选项")
    @PostMapping("/company-entity/create")
    @PreAuthorize("@ss.hasPermission('finance:account:update')")
    public CommonResult<Long> createCompanyEntityOption(@Valid @RequestBody FinanceAccountOptionCreateReqVO reqVO) {
        return success(financeAccountService.createCompanyEntityOption(reqVO.getOptionValue()));
    }

    @Operation(summary = "编辑企业主体选项")
    @PutMapping("/company-entity/update")
    @PreAuthorize("@ss.hasPermission('finance:account:update')")
    public CommonResult<Boolean> updateCompanyEntityOption(@Valid @RequestBody FinanceAccountOptionUpdateReqVO reqVO) {
        return success(financeAccountService.updateCompanyEntityOption(reqVO.getId(), reqVO.getOptionValue()));
    }

    @Operation(summary = "删除企业主体选项")
    @DeleteMapping("/company-entity/delete")
    @PreAuthorize("@ss.hasPermission('finance:account:update')")
    public CommonResult<Boolean> deleteCompanyEntityOption(@RequestParam("id") Long id) {
        return success(financeAccountService.deleteCompanyEntityOption(id));
    }

    @Operation(summary = "查询账户标签选项")
    @GetMapping("/account-tag/simple-list")
    @PreAuthorize("@ss.hasAnyPermissions('finance:account:query,finance:account:list')")
    public CommonResult<List<String>> getAccountTagOptions() {
        return success(financeAccountService.getAccountTagOptions());
    }

    @Operation(summary = "查询银行支行选项")
    @GetMapping("/bank-branch/simple-list")
    @PreAuthorize("@ss.hasAnyPermissions('finance:account:query,finance:account:list')")
    public CommonResult<List<String>> getBankBranchOptions(@RequestParam(value = "keyword", required = false) String keyword) {
        return success(financeAccountService.getBankBranchOptions(keyword));
    }

    @Operation(summary = "查询银行支行选项列表")
    @GetMapping("/bank-branch/list")
    @PreAuthorize("@ss.hasAnyPermissions('finance:account:query,finance:account:list')")
    public CommonResult<List<FinanceAccountOptionRespVO>> getBankBranchOptionList(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(BeanUtils.toBean(financeAccountService.getBankBranchOptionList(keyword),
                FinanceAccountOptionRespVO.class));
    }

    @Operation(summary = "新增银行支行选项")
    @PostMapping("/bank-branch/create")
    @PreAuthorize("@ss.hasPermission('finance:account:update')")
    public CommonResult<Long> createBankBranchOption(@Valid @RequestBody FinanceAccountOptionCreateReqVO reqVO) {
        return success(financeAccountService.createBankBranchOption(reqVO.getOptionValue()));
    }

    @Operation(summary = "编辑银行支行选项")
    @PutMapping("/bank-branch/update")
    @PreAuthorize("@ss.hasPermission('finance:account:update')")
    public CommonResult<Boolean> updateBankBranchOption(@Valid @RequestBody FinanceAccountOptionUpdateReqVO reqVO) {
        return success(financeAccountService.updateBankBranchOption(reqVO.getId(), reqVO.getOptionValue()));
    }

    @Operation(summary = "删除银行支行选项")
    @DeleteMapping("/bank-branch/delete")
    @PreAuthorize("@ss.hasPermission('finance:account:update')")
    public CommonResult<Boolean> deleteBankBranchOption(@RequestParam("id") Long id) {
        return success(financeAccountService.deleteBankBranchOption(id));
    }
}
