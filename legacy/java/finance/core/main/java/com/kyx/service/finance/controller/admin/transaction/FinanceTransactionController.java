package com.kyx.service.finance.controller.admin.transaction;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionPageReqVO;
import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionRespVO;
import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionSaveReqVO;
import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionUpdateReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceAccountDO;
import com.kyx.service.finance.service.init.FinanceAccountService;
import com.kyx.service.finance.service.transaction.FinanceTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 资金流水 Controller
 *
 * @author xyang
 */
@RestController
@RequestMapping("/finance/transaction")
@Tag(name = "财务管理 - 资金流水")
@Validated
public class FinanceTransactionController {

    @Resource
    private FinanceTransactionService financeTransactionService;
    @Resource
    private FinanceAccountService financeAccountService;

    @Operation(summary = "新增流水")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('finance:transaction:create')")
    public CommonResult<Long> createTransaction(@Valid @RequestBody FinanceTransactionSaveReqVO reqVO) {
        return success(financeTransactionService.createTransaction(reqVO));
    }

    @Operation(summary = "更新流水")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('finance:transaction:update')")
    public CommonResult<Boolean> updateTransaction(@Valid @RequestBody FinanceTransactionUpdateReqVO reqVO) {
        return success(financeTransactionService.updateTransaction(reqVO));
    }

    @Operation(summary = "删除流水")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('finance:transaction:delete')")
    public CommonResult<Boolean> deleteTransaction(@RequestParam Long id) {
        return success(financeTransactionService.deleteTransaction(id));
    }

    @Operation(summary = "作废流水")
    @PostMapping({"/reverse", "/void"})
    @PreAuthorize("@ss.hasAnyPermissions('finance:transaction:reverse,finance:transaction:void')")
    public CommonResult<Boolean> reverseTransaction(@RequestParam Long id) {
        return success(financeTransactionService.reverseTransaction(id));
    }

    @Operation(summary = "获取流水详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasAnyPermissions('finance:transaction:query,finance:transaction:update')")
    public CommonResult<FinanceTransactionRespVO> getTransaction(@RequestParam Long id) {
        FinanceTransactionRespVO respVO = BeanUtils.toBean(financeTransactionService.getTransaction(id), FinanceTransactionRespVO.class);
        enrichAccountInfo(respVO, null);
        return success(respVO);
    }

    @Operation(summary = "分页查询流水")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasAnyPermissions('finance:transaction:list,finance:receivable-payable:list,finance:receivable-payable:write-off:create,finance:receivable-payable:writeoff')")
    public CommonResult<PageResult<FinanceTransactionRespVO>> pageTransaction(@Valid FinanceTransactionPageReqVO reqVO) {
        PageResult<FinanceTransactionRespVO> pageResult =
                BeanUtils.toBean(financeTransactionService.pageTransaction(reqVO), FinanceTransactionRespVO.class);
        enrichAccountInfo(pageResult.getList());
        return success(pageResult);
    }

    private void enrichAccountInfo(List<FinanceTransactionRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> accountIds = new HashSet<>();
        for (FinanceTransactionRespVO item : list) {
            if (item.getAccountId() != null) {
                accountIds.add(item.getAccountId());
            }
            if (item.getOppositeAccountId() != null) {
                accountIds.add(item.getOppositeAccountId());
            }
        }

        List<FinanceAccountDO> accountList = financeAccountService.getAccountByIds(accountIds);
        Map<Long, FinanceAccountDO> accountMap = CollectionUtils.convertMap(accountList, FinanceAccountDO::getId);
        for (FinanceTransactionRespVO item : list) {
            enrichAccountInfo(item, accountMap);
        }
    }

    private void enrichAccountInfo(FinanceTransactionRespVO item, Map<Long, FinanceAccountDO> accountMap) {
        if (item == null) {
            return;
        }
        FinanceAccountDO mainAccount = fetchAccount(item.getAccountId(), accountMap);
        if (mainAccount != null) {
            item.setAccountName(mainAccount.getBankName())
                    .setAccountNumber(mainAccount.getAccountNumber())
                    .setAccountType(mainAccount.getAccountType());
        }
        FinanceAccountDO oppositeAccount = fetchAccount(item.getOppositeAccountId(), accountMap);
        if (oppositeAccount != null) {
            item.setOppositeAccountName(oppositeAccount.getBankName())
                    .setOppositeAccountNumber(oppositeAccount.getAccountNumber())
                    .setOppositeAccountType(oppositeAccount.getAccountType());
        }
    }

    private FinanceAccountDO fetchAccount(Long accountId, Map<Long, FinanceAccountDO> accountMap) {
        if (accountId == null) {
            return null;
        }
        if (accountMap == null) {
            return financeAccountService.getAccount(accountId);
        }
        return accountMap.get(accountId);
    }
}
