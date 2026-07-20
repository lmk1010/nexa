package com.kyx.service.finance.controller.admin.closing;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.closing.vo.FinanceClosingExecuteReqVO;
import com.kyx.service.finance.controller.admin.closing.vo.FinanceClosingPageReqVO;
import com.kyx.service.finance.controller.admin.closing.vo.FinanceClosingRespVO;
import com.kyx.service.finance.controller.admin.closing.vo.FinanceClosingReverseReqVO;
import com.kyx.service.finance.dal.dataobject.voucher.FinanceVoucherDO;
import com.kyx.service.finance.dal.mysql.voucher.FinanceVoucherMapper;
import com.kyx.service.finance.service.closing.FinanceClosingService;
import com.kyx.service.finance.service.support.FinanceExecutionGuardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 月末结账 Controller
 *
 * @author xyang
 */
@RestController
@RequestMapping("/finance/closing")
@Tag(name = "财务管理 - 月末结账")
@Validated
public class FinanceClosingController {

    private static final long IDEMPOTENT_TIMEOUT_SECONDS = 10L;

    @Resource
    private FinanceClosingService financeClosingService;
    @Resource
    private FinanceExecutionGuardService financeExecutionGuardService;
    @Resource
    private FinanceVoucherMapper financeVoucherMapper;

    @Operation(summary = "分页查询结账记录")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('finance:closing:list')")
    public CommonResult<PageResult<FinanceClosingRespVO>> pageClosing(@Valid FinanceClosingPageReqVO reqVO) {
        PageResult<FinanceClosingRespVO> pageResult =
                BeanUtils.toBean(financeClosingService.pageClosing(reqVO), FinanceClosingRespVO.class);
        enrichProfitTransferVoucher(pageResult);
        return success(pageResult);
    }

    @Operation(summary = "执行月末结账")
    @PostMapping("/close")
    @PreAuthorize("@ss.hasPermission('finance:closing:close')")
    public CommonResult<Long> close(@Valid @RequestBody FinanceClosingExecuteReqVO reqVO) {
        String bizKey = reqVO.getCompanyId() + ":" + reqVO.getClosingPeriod();
        Long id = financeExecutionGuardService.executeWithIdempotentAndRetry(
                "closing:close", bizKey, IDEMPOTENT_TIMEOUT_SECONDS,
                () -> financeClosingService.close(reqVO));
        return success(id);
    }

    @Operation(summary = "反结账")
    @PostMapping("/reverse")
    @PreAuthorize("@ss.hasPermission('finance:closing:reverse')")
    public CommonResult<Boolean> reverse(@Valid @RequestBody FinanceClosingReverseReqVO reqVO) {
        String bizKey = String.valueOf(reqVO.getId());
        Boolean result = financeExecutionGuardService.executeWithIdempotentAndRetry(
                "closing:reverse", bizKey, IDEMPOTENT_TIMEOUT_SECONDS,
                () -> financeClosingService.reverse(reqVO));
        return success(result);
    }

    private void enrichProfitTransferVoucher(PageResult<FinanceClosingRespVO> pageResult) {
        if (pageResult == null || CollectionUtils.isEmpty(pageResult.getList())) {
            return;
        }
        List<FinanceClosingRespVO> list = pageResult.getList();
        Set<Long> voucherIds = list.stream()
                .map(FinanceClosingRespVO::getProfitTransferVoucherId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(voucherIds)) {
            return;
        }
        List<FinanceVoucherDO> voucherList = financeVoucherMapper.selectBatchIds(voucherIds);
        Map<Long, FinanceVoucherDO> voucherMap = CollectionUtils.isEmpty(voucherList)
                ? Collections.emptyMap()
                : voucherList.stream()
                .filter(item -> item != null && item.getId() != null)
                .collect(Collectors.toMap(FinanceVoucherDO::getId, Function.identity(), (a, b) -> a));
        for (FinanceClosingRespVO item : list) {
            if (item == null || item.getProfitTransferVoucherId() == null) {
                continue;
            }
            FinanceVoucherDO voucherDO = voucherMap.get(item.getProfitTransferVoucherId());
            if (voucherDO == null) {
                continue;
            }
            item.setProfitTransferVoucherNo(voucherDO.getVoucherNo());
            item.setProfitTransferVoucherStatus(voucherDO.getStatus());
        }
    }
}
