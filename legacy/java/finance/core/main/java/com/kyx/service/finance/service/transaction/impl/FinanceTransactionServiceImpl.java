package com.kyx.service.finance.service.transaction.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.date.LocalDateTimeUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionPageReqVO;
import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionSaveReqVO;
import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionUpdateReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceAccountDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanySubjectDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceContactDO;
import com.kyx.service.finance.dal.dataobject.transaction.FinanceTransactionDO;
import com.kyx.service.finance.dal.mysql.init.FinanceAccountMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanyMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanySubjectMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceContactMapper;
import com.kyx.service.finance.dal.mysql.transaction.FinanceTransactionMapper;
import com.kyx.service.finance.enums.FinanceTransactionStatusEnum;
import com.kyx.service.finance.enums.FinanceTransactionTypeEnum;
import com.kyx.service.finance.service.transaction.FinanceTransactionService;
import com.kyx.service.finance.service.support.FinancePeriodGuardService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Locale;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.finance.enums.ErrorCodeConstants.*;
import static com.kyx.service.finance.enums.LogRecordConstants.*;

/**
 * 资金流水 Service 实现
 *
 * @author xyang
 */
@Service
@Validated
public class FinanceTransactionServiceImpl implements FinanceTransactionService {

    private static final FinanceTransactionStatusEnum DEFAULT_STATUS = FinanceTransactionStatusEnum.DRAFT;
    private static final String DEFAULT_BUSINESS_MODE = "EXTERNAL";
    private static final String INTERNAL_BUSINESS_MODE = "INTERNAL";
    private static final String DEFAULT_CATEGORY_INCOME = "OTHER_INCOME";
    private static final String DEFAULT_CATEGORY_EXPENSE = "PURCHASE_PAYMENT";
    private static final String DEFAULT_CATEGORY_TRANSFER = "INTERNAL_TRANSFER";
    private static final String DEFAULT_CATEGORY_ALLOCATION = "EXPENSE_ALLOCATION";
    private static final String SUBJECT_TYPE_INCOME = "INCOME";
    private static final String SUBJECT_TYPE_EXPENSE = "EXPENSE";
    private static final String SUBJECT_TYPE_COST = "COST";

    @Resource
    private FinanceTransactionMapper financeTransactionMapper;
    @Resource
    private FinanceAccountMapper financeAccountMapper;
    @Resource
    private FinanceCompanyMapper financeCompanyMapper;
    @Resource
    private FinanceCompanySubjectMapper financeCompanySubjectMapper;
    @Resource
    private FinanceContactMapper financeContactMapper;
    @Resource
    private FinancePeriodGuardService financePeriodGuardService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FINANCE_TRANSACTION_TEMPLATE_TYPE,
            subType = FINANCE_TRANSACTION_TEMPLATE_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}",
            success = FINANCE_TRANSACTION_TEMPLATE_CREATE_SUCCESS)
    public Long createTransaction(FinanceTransactionSaveReqVO reqVO) {
        reqVO.setTransactionNo(StringUtils.trimWhitespace(reqVO.getTransactionNo()));
        reqVO.setCategory(resolveCategoryForCreate(reqVO.getCategory(), reqVO.getTransactionType()));
        FinanceTransactionStatusEnum targetStatus = normalizeStatus(reqVO.getStatus(), DEFAULT_STATUS);
        Long companyId = validateBeforeSave(reqVO, null, null);
        if (FinanceTransactionStatusEnum.INVALID.equals(targetStatus)) {
            throw exception(TRANSACTION_STATUS_TRANSITION_INVALID);
        }
        financePeriodGuardService.validateDateEditable(companyId, reqVO.getTransactionDate());

        FinanceTransactionDO transactionDO = BeanUtils.toBean(reqVO, FinanceTransactionDO.class)
                .setTransactionType(reqVO.getTransactionType())
                .setCompanyId(companyId)
                .setTransactionPeriod(LocalDateTimeUtils.formatSimpleMonth(reqVO.getTransactionDate()));
        if (!FinanceTransactionTypeEnum.TRANSFER.name().equals(reqVO.getTransactionType())) {
            transactionDO.setOppositeAccountId(null);
        }
        transactionDO.setStatus(targetStatus.name());
        transactionDO.setBusinessType(normalizeBusinessMode(reqVO.getBusinessType(), reqVO.getTransactionType()));
        if (transactionDO.getTaxAmount() == null) {
            transactionDO.setTaxAmount(BigDecimal.ZERO);
        }
        if (isBalanceAffected(targetStatus)) {
            applyAccountBalanceChange(reqVO.getTransactionType(), transactionDO);
        }
        financeTransactionMapper.insert(transactionDO);
        return transactionDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FINANCE_TRANSACTION_TEMPLATE_TYPE,
            subType = FINANCE_TRANSACTION_TEMPLATE_UPDATE_SUB_TYPE,
            bizNo = "{{#reqVO.id}}",
            success = FINANCE_TRANSACTION_TEMPLATE_UPDATE_SUCCESS)
    public Boolean updateTransaction(FinanceTransactionUpdateReqVO reqVO) {
        reqVO.setTransactionNo(StringUtils.trimWhitespace(reqVO.getTransactionNo()));
        reqVO.setCategory(StringUtils.trimWhitespace(reqVO.getCategory()));
        FinanceTransactionDO old = financeTransactionMapper.selectById(reqVO.getId());
        if (old == null) {
            throw exception(TRANSACTION_NOT_EXISTS);
        }
        FinanceTransactionStatusEnum oldStatus = parseStatusOrThrow(old.getStatus());
        if (!FinanceTransactionStatusEnum.DRAFT.equals(oldStatus)) {
            throw exception(TRANSACTION_EDIT_ONLY_PENDING);
        }
        financePeriodGuardService.validateDateEditable(old.getCompanyId(), old.getTransactionDate());

        FinanceTransactionStatusEnum targetStatus = normalizeStatus(reqVO.getStatus(), oldStatus);
        Long companyId = validateBeforeSave(reqVO, reqVO.getId(), old.getCompanyId());
        financePeriodGuardService.validateDateEditable(companyId, reqVO.getTransactionDate());
        validateStatusTransition(oldStatus, targetStatus, false);

        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        if (isBalanceAffected(oldStatus)) {
            rollbackAccountBalanceChange(old.getTransactionType(), old);
        }

        FinanceTransactionDO updateDO = BeanUtils.toBean(reqVO, FinanceTransactionDO.class)
                .setId(reqVO.getId())
                .setCompanyId(companyId)
                .setTransactionType(reqVO.getTransactionType())
                .setTransactionPeriod(LocalDateTimeUtils.formatSimpleMonth(reqVO.getTransactionDate()))
                .setStatus(targetStatus.name())
                .setBusinessType(normalizeBusinessMode(reqVO.getBusinessType(), reqVO.getTransactionType()));
        if (!FinanceTransactionTypeEnum.TRANSFER.name().equals(reqVO.getTransactionType())) {
            updateDO.setOppositeAccountId(null);
        }
        if (updateDO.getTaxAmount() == null) {
            updateDO.setTaxAmount(BigDecimal.ZERO);
        }
        if (isBalanceAffected(targetStatus)) {
            applyAccountBalanceChange(reqVO.getTransactionType(), updateDO);
        }
        boolean updated = financeTransactionMapper.update(updateDO, new LambdaUpdateWrapper<FinanceTransactionDO>()
                .eq(FinanceTransactionDO::getId, reqVO.getId())
                .eq(FinanceTransactionDO::getStatus, oldStatus.name())) > 0;
        if (!updated) {
            throw exception(TRANSACTION_STATUS_TRANSITION_INVALID);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FINANCE_TRANSACTION_TEMPLATE_TYPE,
            subType = FINANCE_TRANSACTION_TEMPLATE_DELETE_SUB_TYPE,
            bizNo = "{{#id}}",
            success = FINANCE_TRANSACTION_TEMPLATE_DELETE_SUCCESS)
    public Boolean deleteTransaction(Long id) {
        FinanceTransactionDO transactionDO = financeTransactionMapper.selectById(id);
        if (transactionDO == null) {
            return false;
        }
        financePeriodGuardService.validateDateEditable(transactionDO.getCompanyId(), transactionDO.getTransactionDate());
        FinanceTransactionStatusEnum oldStatus = parseStatusOrThrow(transactionDO.getStatus());
        if (!FinanceTransactionStatusEnum.DRAFT.equals(oldStatus)) {
            throw exception(TRANSACTION_DELETE_ONLY_PENDING);
        }
        int deletedRows = financeTransactionMapper.delete(new LambdaQueryWrapper<FinanceTransactionDO>()
                .eq(FinanceTransactionDO::getId, id)
                .eq(FinanceTransactionDO::getStatus, FinanceTransactionStatusEnum.DRAFT.name()));
        if (deletedRows <= 0) {
            throw exception(TRANSACTION_STATUS_TRANSITION_INVALID);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FINANCE_TRANSACTION_TEMPLATE_TYPE,
            subType = FINANCE_TRANSACTION_TEMPLATE_REVERSE_SUB_TYPE,
            bizNo = "{{#id}}",
            success = FINANCE_TRANSACTION_TEMPLATE_REVERSE_SUCCESS)
    public Boolean reverseTransaction(Long id) {
        FinanceTransactionDO transactionDO = financeTransactionMapper.selectById(id);
        if (transactionDO == null) {
            throw exception(TRANSACTION_NOT_EXISTS);
        }
        financePeriodGuardService.validateDateEditable(transactionDO.getCompanyId(), transactionDO.getTransactionDate());
        FinanceTransactionStatusEnum oldStatus = parseStatusOrThrow(transactionDO.getStatus());
        if (FinanceTransactionStatusEnum.INVALID.equals(oldStatus)) {
            throw exception(TRANSACTION_REVERSED_NOT_OPERABLE);
        }
        if (!FinanceTransactionStatusEnum.SUCCESS.equals(oldStatus)) {
            throw exception(TRANSACTION_REVERSE_ONLY_SUCCESS);
        }
        validateStatusTransition(oldStatus, FinanceTransactionStatusEnum.INVALID, true);

        if (isBalanceAffected(oldStatus)) {
            rollbackAccountBalanceChange(transactionDO.getTransactionType(), transactionDO);
        }

        FinanceTransactionDO updateDO = new FinanceTransactionDO()
                .setId(id)
                .setStatus(FinanceTransactionStatusEnum.INVALID.name());
        boolean updated = financeTransactionMapper.update(updateDO, new LambdaUpdateWrapper<FinanceTransactionDO>()
                .eq(FinanceTransactionDO::getId, id)
                .eq(FinanceTransactionDO::getStatus, oldStatus.name())) > 0;
        if (!updated) {
            throw exception(TRANSACTION_STATUS_TRANSITION_INVALID);
        }
        return true;
    }

    @Override
    public FinanceTransactionDO getTransaction(Long id) {
        FinanceTransactionDO transactionDO = financeTransactionMapper.selectById(id);
        if (transactionDO == null) {
            throw exception(TRANSACTION_NOT_EXISTS);
        }
        return transactionDO;
    }

    @Override
    public PageResult<FinanceTransactionDO> pageTransaction(FinanceTransactionPageReqVO reqVO) {
        return financeTransactionMapper.selectPage(reqVO);
    }

    private Long validateBeforeSave(FinanceTransactionSaveReqVO reqVO, Long excludeId, Long fixedCompanyId) {
        reqVO.setSubjectCode(StringUtils.trimWhitespace(reqVO.getSubjectCode()));
        // 账户和往来对象为租户级共享主数据，流水仅校验引用对象存在，
        // 账套归属由 companyId 控制。
        validateAccountActive(reqVO.getAccountId());
        validateContactExists(reqVO.getContactId());
        Long companyId = fixedCompanyId == null ? reqVO.getCompanyId() : fixedCompanyId;
        if (companyId == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
        if (fixedCompanyId != null && reqVO.getCompanyId() != null && !reqVO.getCompanyId().equals(fixedCompanyId)) {
            throw exception(TRANSACTION_STATUS_TRANSITION_INVALID);
        }
        if (financeCompanyMapper.selectById(companyId) == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
        if (financeTransactionMapper.existsByTransactionNo(companyId, reqVO.getTransactionNo(), excludeId)) {
            throw exception(TRANSACTION_NO_EXISTS);
        }

        boolean isTransfer = FinanceTransactionTypeEnum.TRANSFER.name().equals(reqVO.getTransactionType());
        if (!isTransfer) {
            FinanceCompanySubjectDO subjectDO = financeCompanySubjectMapper
                    .selectEnabledByCompanyIdAndSubjectCode(companyId, reqVO.getSubjectCode());
            if (subjectDO == null) {
                throw exception(TRANSACTION_SUBJECT_REQUIRED);
            }
            if (!isSubjectTypeMatch(reqVO.getTransactionType(), subjectDO.getSubjectType())) {
                throw exception(TRANSACTION_SUBJECT_TYPE_MISMATCH);
            }
        }
        if (isTransfer) {
            reqVO.setSubjectCode(null);
            if (reqVO.getOppositeAccountId() == null) {
                throw exception(TRANSACTION_OPPOSITE_ACCOUNT_REQUIRED);
            }
            if (reqVO.getAccountId().equals(reqVO.getOppositeAccountId())) {
                throw exception(TRANSACTION_TRANSFER_ACCOUNT_SAME);
            }
            validateAccountActive(reqVO.getOppositeAccountId());
        } else {
            reqVO.setOppositeAccountId(null);
        }
        return companyId;
    }

    private void validateAccountActive(Long accountId) {
        FinanceAccountDO account = financeAccountMapper.selectById(accountId);
        if (account == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        if (account.getStatus() != null && !CommonStatusEnum.isEnable(account.getStatus())) {
            throw exception(ACCOUNT_DISABLED);
        }
    }

    private void validateContactExists(Long contactId) {
        if (contactId == null) {
            return;
        }
        FinanceContactDO contact = financeContactMapper.selectById(contactId);
        if (contact == null) {
            throw exception(CONTACT_NOT_EXISTS);
        }
        if (contact.getStatus() != null && !CommonStatusEnum.isEnable(contact.getStatus())) {
            throw exception(CONTACT_DISABLED);
        }
    }

    private void validateStatusTransition(FinanceTransactionStatusEnum oldStatus,
                                          FinanceTransactionStatusEnum newStatus,
                                          boolean allowToVoid) {
        if (FinanceTransactionStatusEnum.INVALID.equals(oldStatus)
                && !FinanceTransactionStatusEnum.INVALID.equals(newStatus)) {
            throw exception(TRANSACTION_STATUS_TRANSITION_INVALID);
        }
        if (!allowToVoid && FinanceTransactionStatusEnum.INVALID.equals(newStatus)) {
            throw exception(TRANSACTION_STATUS_TRANSITION_INVALID);
        }
    }

    private FinanceTransactionStatusEnum normalizeStatus(String status, FinanceTransactionStatusEnum defaultStatus) {
        if (!StringUtils.hasText(status)) {
            return defaultStatus;
        }
        return parseStatusOrThrow(status);
    }

    private boolean isBalanceAffected(FinanceTransactionStatusEnum status) {
        return FinanceTransactionStatusEnum.SUCCESS.equals(status);
    }

    private FinanceTransactionStatusEnum parseStatusOrThrow(String status) {
        if (!StringUtils.hasText(status)) {
            throw exception(TRANSACTION_STATUS_INVALID);
        }
        try {
            String normalized = StringUtils.trimWhitespace(status).toUpperCase(Locale.ROOT);
            return FinanceTransactionStatusEnum.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw exception(TRANSACTION_STATUS_INVALID);
        }
    }

    private String normalizeBusinessMode(String businessMode, String transactionType) {
        if (FinanceTransactionTypeEnum.ALLOCATION.name().equals(transactionType)) {
            return INTERNAL_BUSINESS_MODE;
        }
        if (!StringUtils.hasText(businessMode)) {
            return DEFAULT_BUSINESS_MODE;
        }
        String normalized = StringUtils.trimWhitespace(businessMode).toUpperCase(Locale.ROOT);
        if ("INTERNAL".equals(normalized) || "EXTERNAL".equals(normalized)) {
            return normalized;
        }
        return DEFAULT_BUSINESS_MODE;
    }

    private void applyAccountBalanceChange(String transactionType, FinanceTransactionDO transactionDO) {
        BigDecimal amount = transactionDO.getAmount();
        if (FinanceTransactionTypeEnum.TRANSFER.name().equals(transactionType)) {
            decreaseBalanceOrThrow(transactionDO.getAccountId(), amount);
            if (financeAccountMapper.increaseBalance(transactionDO.getOppositeAccountId(), amount) <= 0) {
                throw exception(ACCOUNT_NOT_EXISTS);
            }
            return;
        }
        if (FinanceTransactionTypeEnum.INCOME.name().equals(transactionType)) {
            if (financeAccountMapper.increaseBalance(transactionDO.getAccountId(), amount) <= 0) {
                throw exception(ACCOUNT_NOT_EXISTS);
            }
            return;
        }
        if (FinanceTransactionTypeEnum.EXPENSE.name().equals(transactionType)) {
            decreaseBalanceOrThrow(transactionDO.getAccountId(), amount);
            return;
        }
        if (FinanceTransactionTypeEnum.ALLOCATION.name().equals(transactionType)) {
            return;
        }
    }

    private void rollbackAccountBalanceChange(String transactionType, FinanceTransactionDO transactionDO) {
        BigDecimal amount = transactionDO.getAmount();
        if (FinanceTransactionTypeEnum.TRANSFER.name().equals(transactionType)) {
            if (financeAccountMapper.increaseBalance(transactionDO.getAccountId(), amount) <= 0) {
                throw exception(ACCOUNT_NOT_EXISTS);
            }
            decreaseBalanceOrThrow(transactionDO.getOppositeAccountId(), amount);
            return;
        }
        if (FinanceTransactionTypeEnum.INCOME.name().equals(transactionType)) {
            decreaseBalanceOrThrow(transactionDO.getAccountId(), amount);
            return;
        }
        if (FinanceTransactionTypeEnum.EXPENSE.name().equals(transactionType)) {
            if (financeAccountMapper.increaseBalance(transactionDO.getAccountId(), amount) <= 0) {
                throw exception(ACCOUNT_NOT_EXISTS);
            }
            return;
        }
        if (FinanceTransactionTypeEnum.ALLOCATION.name().equals(transactionType)) {
            return;
        }
    }

    private void decreaseBalanceOrThrow(Long accountId, BigDecimal amount) {
        if (financeAccountMapper.decreaseBalance(accountId, amount) > 0) {
            return;
        }
        if (financeAccountMapper.selectById(accountId) == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        throw exception(ACCOUNT_BALANCE_NOT_ENOUGH);
    }

    private boolean isSubjectTypeMatch(String transactionType, String subjectType) {
        if (!StringUtils.hasText(transactionType) || !StringUtils.hasText(subjectType)) {
            return false;
        }
        String normalizedTransactionType = StringUtils.trimWhitespace(transactionType).toUpperCase(Locale.ROOT);
        String normalizedSubjectType = StringUtils.trimWhitespace(subjectType).toUpperCase(Locale.ROOT);
        if (FinanceTransactionTypeEnum.INCOME.name().equals(normalizedTransactionType)) {
            return SUBJECT_TYPE_INCOME.equals(normalizedSubjectType);
        }
        if (FinanceTransactionTypeEnum.EXPENSE.name().equals(normalizedTransactionType)) {
            return SUBJECT_TYPE_EXPENSE.equals(normalizedSubjectType)
                    || SUBJECT_TYPE_COST.equals(normalizedSubjectType);
        }
        if (FinanceTransactionTypeEnum.ALLOCATION.name().equals(normalizedTransactionType)) {
            return SUBJECT_TYPE_EXPENSE.equals(normalizedSubjectType)
                    || SUBJECT_TYPE_COST.equals(normalizedSubjectType);
        }
        return true;
    }

    private String resolveCategoryForCreate(String category, String transactionType) {
        String normalized = StringUtils.trimWhitespace(category);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (!StringUtils.hasText(transactionType)) {
            return normalized;
        }
        String normalizedTransactionType = StringUtils.trimWhitespace(transactionType).toUpperCase(Locale.ROOT);
        if (FinanceTransactionTypeEnum.INCOME.name().equals(normalizedTransactionType)) {
            return DEFAULT_CATEGORY_INCOME;
        }
        if (FinanceTransactionTypeEnum.EXPENSE.name().equals(normalizedTransactionType)) {
            return DEFAULT_CATEGORY_EXPENSE;
        }
        if (FinanceTransactionTypeEnum.TRANSFER.name().equals(normalizedTransactionType)) {
            return DEFAULT_CATEGORY_TRANSFER;
        }
        if (FinanceTransactionTypeEnum.ALLOCATION.name().equals(normalizedTransactionType)) {
            return DEFAULT_CATEGORY_ALLOCATION;
        }
        return normalized;
    }
}
