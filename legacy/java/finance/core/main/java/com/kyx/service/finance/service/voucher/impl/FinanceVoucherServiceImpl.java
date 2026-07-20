package com.kyx.service.finance.service.voucher.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.date.LocalDateTimeUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherDetailSaveReqVO;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherPageReqVO;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherSaveReqVO;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherUpdateReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceAccountDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceContactDO;
import com.kyx.service.finance.dal.dataobject.voucher.FinanceVoucherDO;
import com.kyx.service.finance.dal.dataobject.voucher.FinanceVoucherDetailDO;
import com.kyx.service.finance.dal.dataobject.voucher.FinanceVoucherDetailHistoryDO;
import com.kyx.service.finance.dal.mysql.init.FinanceAccountMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanyMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceContactMapper;
import com.kyx.service.finance.dal.mysql.voucher.FinanceVoucherDetailMapper;
import com.kyx.service.finance.dal.mysql.voucher.FinanceVoucherDetailHistoryMapper;
import com.kyx.service.finance.dal.mysql.voucher.FinanceVoucherMapper;
import com.kyx.service.finance.enums.FinanceVoucherStatusEnum;
import com.kyx.service.finance.service.voucher.FinanceVoucherService;
import com.kyx.service.finance.service.support.FinancePeriodGuardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserOperator;
import static com.kyx.service.finance.enums.ErrorCodeConstants.*;

/**
 * 凭证 Service 实现
 *
 * @author xyang
 */
@Service
@Validated
public class FinanceVoucherServiceImpl implements FinanceVoucherService {

    private static final FinanceVoucherStatusEnum DEFAULT_STATUS = FinanceVoucherStatusEnum.DRAFT;
    private static final String DETAIL_HISTORY_OPERATION_DELETE = "DELETE";
    private static final String DETAIL_HISTORY_OPERATION_UPDATE = "UPDATE";

    @Resource
    private FinanceVoucherMapper financeVoucherMapper;
    @Resource
    private FinanceVoucherDetailMapper financeVoucherDetailMapper;
    @Resource
    private FinanceVoucherDetailHistoryMapper financeVoucherDetailHistoryMapper;
    @Resource
    private FinanceAccountMapper financeAccountMapper;
    @Resource
    private FinanceContactMapper financeContactMapper;
    @Resource
    private FinanceCompanyMapper financeCompanyMapper;
    @Resource
    private FinancePeriodGuardService financePeriodGuardService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVoucher(FinanceVoucherSaveReqVO reqVO) {
        reqVO.setVoucherNo(StringUtils.trimWhitespace(reqVO.getVoucherNo()));
        FinanceVoucherStatusEnum targetStatus = normalizeStatus(reqVO.getStatus(), DEFAULT_STATUS);
        validateEditableStatus(targetStatus);
        Long companyId = resolveCompanyId(reqVO.getCompanyId(), null);
        financePeriodGuardService.validateDateEditable(companyId, reqVO.getVoucherDate());
        validateVoucherNoUnique(companyId, reqVO.getVoucherNo(), null);

        VoucherAmountSummary amountSummary = validateDetails(reqVO.getDetails());
        FinanceVoucherDO voucherDO = BeanUtils.toBean(reqVO, FinanceVoucherDO.class)
                .setCompanyId(companyId)
                .setVoucherNo(reqVO.getVoucherNo())
                .setVoucherPeriod(LocalDateTimeUtils.formatSimpleMonth(reqVO.getVoucherDate()))
                .setStatus(targetStatus.name())
                .setTotalDebit(amountSummary.totalDebit)
                .setTotalCredit(amountSummary.totalCredit);
        financeVoucherMapper.insert(voucherDO);
        saveVoucherDetails(voucherDO.getId(), companyId, reqVO.getDetails());
        return voucherDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateVoucher(FinanceVoucherUpdateReqVO reqVO) {
        reqVO.setVoucherNo(StringUtils.trimWhitespace(reqVO.getVoucherNo()));
        FinanceVoucherDO old = financeVoucherMapper.selectById(reqVO.getId());
        if (old == null) {
            throw exception(VOUCHER_NOT_EXISTS);
        }
        FinanceVoucherStatusEnum oldStatus = parseStatusOrThrow(old.getStatus());
        if (!FinanceVoucherStatusEnum.DRAFT.equals(oldStatus)) {
            throw exception(VOUCHER_EDIT_ONLY_DRAFT);
        }
        financePeriodGuardService.validateDateEditable(old.getCompanyId(), old.getVoucherDate());

        FinanceVoucherStatusEnum targetStatus = normalizeStatus(reqVO.getStatus(), oldStatus);
        validateEditableStatus(targetStatus);
        Long companyId = resolveCompanyId(reqVO.getCompanyId(), old.getCompanyId());
        financePeriodGuardService.validateDateEditable(companyId, reqVO.getVoucherDate());
        validateVoucherNoUnique(companyId, reqVO.getVoucherNo(), reqVO.getId());

        VoucherAmountSummary amountSummary = validateDetails(reqVO.getDetails());
        FinanceVoucherDO updateDO = BeanUtils.toBean(reqVO, FinanceVoucherDO.class)
                .setId(reqVO.getId())
                .setCompanyId(companyId)
                .setVoucherNo(reqVO.getVoucherNo())
                .setVoucherPeriod(LocalDateTimeUtils.formatSimpleMonth(reqVO.getVoucherDate()))
                .setStatus(targetStatus.name())
                .setTotalDebit(amountSummary.totalDebit)
                .setTotalCredit(amountSummary.totalCredit);

        boolean updated = financeVoucherMapper.update(updateDO, new LambdaUpdateWrapper<FinanceVoucherDO>()
                .eq(FinanceVoucherDO::getId, reqVO.getId())
                .eq(FinanceVoucherDO::getStatus, oldStatus.name())) > 0;
        if (!updated) {
            throw exception(VOUCHER_STATUS_TRANSITION_INVALID);
        }
        List<FinanceVoucherDetailDO> oldDetails = financeVoucherDetailMapper.selectListByVoucherId(reqVO.getId());
        archiveVoucherDetails(oldDetails, DETAIL_HISTORY_OPERATION_UPDATE);
        financeVoucherDetailMapper.deleteByVoucherId(reqVO.getId());
        saveVoucherDetails(reqVO.getId(), companyId, reqVO.getDetails());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteVoucher(Long id) {
        FinanceVoucherDO voucherDO = financeVoucherMapper.selectById(id);
        if (voucherDO == null) {
            return false;
        }
        financePeriodGuardService.validateDateEditable(voucherDO.getCompanyId(), voucherDO.getVoucherDate());
        FinanceVoucherStatusEnum status = parseStatusOrThrow(voucherDO.getStatus());
        if (!FinanceVoucherStatusEnum.DRAFT.equals(status)) {
            throw exception(VOUCHER_DELETE_ONLY_DRAFT);
        }
        List<FinanceVoucherDetailDO> oldDetails = financeVoucherDetailMapper.selectListByVoucherId(id);
        archiveVoucherDetails(oldDetails, DETAIL_HISTORY_OPERATION_DELETE);
        int deletedRows = financeVoucherMapper.delete(new LambdaQueryWrapper<FinanceVoucherDO>()
                .eq(FinanceVoucherDO::getId, id)
                .eq(FinanceVoucherDO::getStatus, FinanceVoucherStatusEnum.DRAFT.name()));
        if (deletedRows <= 0) {
            throw exception(VOUCHER_STATUS_TRANSITION_INVALID);
        }
        financeVoucherDetailMapper.deleteByVoucherId(id);
        return true;
    }

    @Override
    public FinanceVoucherDO getVoucher(Long id) {
        FinanceVoucherDO voucherDO = financeVoucherMapper.selectById(id);
        if (voucherDO == null) {
            throw exception(VOUCHER_NOT_EXISTS);
        }
        return voucherDO;
    }

    @Override
    public List<FinanceVoucherDetailDO> getVoucherDetails(Long voucherId) {
        return financeVoucherDetailMapper.selectListByVoucherId(voucherId);
    }

    @Override
    public PageResult<FinanceVoucherDO> pageVoucher(FinanceVoucherPageReqVO reqVO) {
        return financeVoucherMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean postVoucher(Long id) {
        FinanceVoucherDO voucherDO = financeVoucherMapper.selectById(id);
        if (voucherDO == null) {
            throw exception(VOUCHER_NOT_EXISTS);
        }
        financePeriodGuardService.validateDateEditable(voucherDO.getCompanyId(), voucherDO.getVoucherDate());
        FinanceVoucherStatusEnum oldStatus = parseStatusOrThrow(voucherDO.getStatus());
        if (FinanceVoucherStatusEnum.POSTED.equals(oldStatus)) {
            return true;
        }
        if (FinanceVoucherStatusEnum.VOID.equals(oldStatus)) {
            throw exception(VOUCHER_POST_NOT_ALLOWED);
        }
        if (!(FinanceVoucherStatusEnum.DRAFT.equals(oldStatus) || FinanceVoucherStatusEnum.APPROVED.equals(oldStatus))) {
            throw exception(VOUCHER_POST_NOT_ALLOWED);
        }
        FinanceVoucherDO updateDO = new FinanceVoucherDO()
                .setId(id)
                .setStatus(FinanceVoucherStatusEnum.POSTED.name())
                .setPostedBy(getLoginUserOperator())
                .setPostedTime(LocalDateTime.now());
        boolean updated = financeVoucherMapper.update(updateDO, new LambdaUpdateWrapper<FinanceVoucherDO>()
                .eq(FinanceVoucherDO::getId, id)
                .eq(FinanceVoucherDO::getStatus, oldStatus.name())) > 0;
        if (!updated) {
            throw exception(VOUCHER_STATUS_TRANSITION_INVALID);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean voidVoucher(Long id) {
        FinanceVoucherDO voucherDO = financeVoucherMapper.selectById(id);
        if (voucherDO == null) {
            throw exception(VOUCHER_NOT_EXISTS);
        }
        financePeriodGuardService.validateDateEditable(voucherDO.getCompanyId(), voucherDO.getVoucherDate());
        FinanceVoucherStatusEnum oldStatus = parseStatusOrThrow(voucherDO.getStatus());
        if (FinanceVoucherStatusEnum.VOID.equals(oldStatus)) {
            return true;
        }
        if (FinanceVoucherStatusEnum.POSTED.equals(oldStatus)) {
            throw exception(VOUCHER_VOID_NOT_ALLOWED);
        }
        if (!(FinanceVoucherStatusEnum.DRAFT.equals(oldStatus) || FinanceVoucherStatusEnum.APPROVED.equals(oldStatus))) {
            throw exception(VOUCHER_VOID_NOT_ALLOWED);
        }
        FinanceVoucherDO updateDO = new FinanceVoucherDO()
                .setId(id)
                .setStatus(FinanceVoucherStatusEnum.VOID.name());
        boolean updated = financeVoucherMapper.update(updateDO, new LambdaUpdateWrapper<FinanceVoucherDO>()
                .eq(FinanceVoucherDO::getId, id)
                .eq(FinanceVoucherDO::getStatus, oldStatus.name())) > 0;
        if (!updated) {
            throw exception(VOUCHER_STATUS_TRANSITION_INVALID);
        }
        return true;
    }

    private void saveVoucherDetails(Long voucherId, Long companyId, List<FinanceVoucherDetailSaveReqVO> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        List<FinanceVoucherDetailDO> detailDOList = new ArrayList<>(details.size());
        for (int i = 0; i < details.size(); i++) {
            FinanceVoucherDetailSaveReqVO item = details.get(i);
            FinanceVoucherDetailDO detailDO = BeanUtils.toBean(item, FinanceVoucherDetailDO.class)
                    .setCompanyId(companyId)
                    .setVoucherId(voucherId)
                    .setLineNo(i + 1)
                    .setDebitAmount(safeAmount(item.getDebitAmount()))
                    .setCreditAmount(safeAmount(item.getCreditAmount()))
                    .setTaxAmount(safeAmount(item.getTaxAmount()));
            detailDOList.add(detailDO);
        }
        financeVoucherDetailMapper.insertBatch(detailDOList);
    }

    private void archiveVoucherDetails(List<FinanceVoucherDetailDO> details, String operationType) {
        if (details == null || details.isEmpty()) {
            return;
        }
        LocalDateTime operationTime = LocalDateTime.now();
        List<FinanceVoucherDetailHistoryDO> historyDOList = new ArrayList<>(details.size());
        for (FinanceVoucherDetailDO detail : details) {
            FinanceVoucherDetailHistoryDO historyDO = BeanUtils.toBean(detail, FinanceVoucherDetailHistoryDO.class)
                    .setId(null)
                    .setDetailId(detail.getId())
                    .setOperationType(operationType)
                    .setOperationTime(operationTime);
            historyDOList.add(historyDO);
        }
        financeVoucherDetailHistoryMapper.insertBatch(historyDOList);
    }

    private void validateVoucherNoUnique(Long companyId, String voucherNo, Long excludeId) {
        if (financeVoucherMapper.existsByVoucherNo(companyId, voucherNo, excludeId)) {
            throw exception(VOUCHER_NO_EXISTS);
        }
    }

    private Long resolveCompanyId(Long reqCompanyId, Long fallbackCompanyId) {
        Long companyId = reqCompanyId == null ? fallbackCompanyId : reqCompanyId;
        if (companyId == null || financeCompanyMapper.selectById(companyId) == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
        return companyId;
    }

    private VoucherAmountSummary validateDetails(List<FinanceVoucherDetailSaveReqVO> details) {
        if (details == null || details.isEmpty()) {
            throw exception(VOUCHER_DETAIL_EMPTY);
        }
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (FinanceVoucherDetailSaveReqVO detail : details) {
            validateDetailReference(detail);
            BigDecimal debit = safeAmount(detail.getDebitAmount());
            BigDecimal credit = safeAmount(detail.getCreditAmount());
            if (debit.compareTo(BigDecimal.ZERO) < 0 || credit.compareTo(BigDecimal.ZERO) < 0) {
                throw exception(VOUCHER_DETAIL_AMOUNT_INVALID);
            }
            boolean hasDebit = debit.compareTo(BigDecimal.ZERO) > 0;
            boolean hasCredit = credit.compareTo(BigDecimal.ZERO) > 0;
            if (hasDebit == hasCredit) {
                throw exception(VOUCHER_DETAIL_AMOUNT_INVALID);
            }
            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
        }
        if (totalDebit.compareTo(BigDecimal.ZERO) <= 0 || totalDebit.compareTo(totalCredit) != 0) {
            throw exception(VOUCHER_DEBIT_CREDIT_NOT_BALANCED);
        }
        return new VoucherAmountSummary(totalDebit, totalCredit);
    }

    private void validateDetailReference(FinanceVoucherDetailSaveReqVO detail) {
        if (detail.getAccountId() != null) {
            FinanceAccountDO account = financeAccountMapper.selectById(detail.getAccountId());
            if (account == null) {
                throw exception(ACCOUNT_NOT_EXISTS);
            }
            if (account.getStatus() != null && !CommonStatusEnum.isEnable(account.getStatus())) {
                throw exception(ACCOUNT_DISABLED);
            }
        }
        if (detail.getContactId() != null) {
            FinanceContactDO contact = financeContactMapper.selectById(detail.getContactId());
            if (contact == null) {
                throw exception(CONTACT_NOT_EXISTS);
            }
            if (contact.getStatus() != null && !CommonStatusEnum.isEnable(contact.getStatus())) {
                throw exception(CONTACT_DISABLED);
            }
        }
    }

    private void validateEditableStatus(FinanceVoucherStatusEnum status) {
        if (!(FinanceVoucherStatusEnum.DRAFT.equals(status) || FinanceVoucherStatusEnum.APPROVED.equals(status))) {
            throw exception(VOUCHER_STATUS_TRANSITION_INVALID);
        }
    }

    private FinanceVoucherStatusEnum normalizeStatus(String status, FinanceVoucherStatusEnum defaultStatus) {
        if (!StringUtils.hasText(status)) {
            return defaultStatus;
        }
        return parseStatusOrThrow(status);
    }

    private FinanceVoucherStatusEnum parseStatusOrThrow(String status) {
        if (!StringUtils.hasText(status)) {
            throw exception(VOUCHER_STATUS_INVALID);
        }
        try {
            String normalized = StringUtils.trimWhitespace(status).toUpperCase(Locale.ROOT);
            return FinanceVoucherStatusEnum.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw exception(VOUCHER_STATUS_INVALID);
        }
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static class VoucherAmountSummary {
        private final BigDecimal totalDebit;
        private final BigDecimal totalCredit;

        private VoucherAmountSummary(BigDecimal totalDebit, BigDecimal totalCredit) {
            this.totalDebit = totalDebit;
            this.totalCredit = totalCredit;
        }
    }
}
