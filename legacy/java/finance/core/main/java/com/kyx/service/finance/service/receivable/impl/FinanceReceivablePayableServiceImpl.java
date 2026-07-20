package com.kyx.service.finance.service.receivable.impl;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayablePageReqVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableSaveReqVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableUpdateReqVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableWriteOffPageReqVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableWriteOffSaveReqVO;
import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionSaveReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceContactDO;
import com.kyx.service.finance.dal.dataobject.receivable.FinanceReceivablePayableDO;
import com.kyx.service.finance.dal.dataobject.receivable.FinanceReceivablePayableDetailDO;
import com.kyx.service.finance.dal.dataobject.transaction.FinanceTransactionDO;
import com.kyx.service.finance.dal.mysql.init.FinanceContactMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanyMapper;
import com.kyx.service.finance.dal.mysql.receivable.FinanceReceivablePayableDetailMapper;
import com.kyx.service.finance.dal.mysql.receivable.FinanceReceivablePayableMapper;
import com.kyx.service.finance.dal.mysql.transaction.FinanceTransactionMapper;
import com.kyx.service.finance.enums.FinanceReceivablePayableStatusEnum;
import com.kyx.service.finance.enums.FinanceReceivablePayableTypeEnum;
import com.kyx.service.finance.enums.FinanceTransactionStatusEnum;
import com.kyx.service.finance.enums.FinanceTransactionTypeEnum;
import com.kyx.service.finance.service.receivable.FinanceReceivablePayableService;
import com.kyx.service.finance.service.support.FinancePeriodGuardService;
import com.kyx.service.finance.service.transaction.FinanceTransactionService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.finance.enums.ErrorCodeConstants.*;
import static com.kyx.service.finance.enums.LogRecordConstants.*;

/**
 * 往来账 Service 实现
 * @author xyang
 */
@Service
@Validated
public class FinanceReceivablePayableServiceImpl implements FinanceReceivablePayableService {

    private static final String DEFAULT_RECEIVABLE_WRITE_OFF_REF_TYPE = "SALES_RECEIPT";
    private static final String DEFAULT_PAYABLE_WRITE_OFF_REF_TYPE = "PURCHASE_PAYMENT";
    private static final String AUTO_TRANSACTION_NO_PREFIX = "WO";
    private static final String AUTO_WRITE_OFF_NO_PREFIX = "WOF";
    private static final DateTimeFormatter AUTO_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int WRITE_OFF_NO_RETRY_TIMES = 8;

    @Resource
    private FinanceReceivablePayableMapper financeReceivablePayableMapper;
    @Resource
    private FinanceReceivablePayableDetailMapper financeReceivablePayableDetailMapper;
    @Resource
    private FinanceContactMapper financeContactMapper;
    @Resource
    private FinanceCompanyMapper financeCompanyMapper;
    @Resource
    private FinanceTransactionService financeTransactionService;
    @Resource
    private FinanceTransactionMapper financeTransactionMapper;
    @Resource
    private FinancePeriodGuardService financePeriodGuardService;

    @Override
    @LogRecord(type = FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_TYPE,
            subType = FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}",
            success = FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_CREATE_SUCCESS)
    public Long createReceivablePayable(FinanceReceivablePayableSaveReqVO reqVO) {
        reqVO.setBillNo(StringUtils.trimWhitespace(reqVO.getBillNo()));
        Long companyId = resolveCompanyId(reqVO.getCompanyId(), null);
        financePeriodGuardService.validateDateEditable(companyId, reqVO.getBillDate());
        validateContactExists(reqVO.getContactId());
        validateBillNoUnique(companyId, reqVO.getBillNo(), null);

        FinanceReceivablePayableDO receivablePayableDO = BeanUtils.toBean(reqVO, FinanceReceivablePayableDO.class);
        receivablePayableDO.setCompanyId(companyId);
        if (receivablePayableDO.getBalance() == null) {
            receivablePayableDO.setBalance(receivablePayableDO.getAmount());
        }
        validateBalance(receivablePayableDO.getAmount(), receivablePayableDO.getBalance());
        receivablePayableDO.setPaidAmount(receivablePayableDO.getAmount().subtract(receivablePayableDO.getBalance()));
        if (!StringUtils.hasText(receivablePayableDO.getStatus())) {
            receivablePayableDO.setStatus(deriveStatus(receivablePayableDO.getAmount(), receivablePayableDO.getBalance()));
        }
        financeReceivablePayableMapper.insert(receivablePayableDO);
        return receivablePayableDO.getId();
    }

    @Override
    @LogRecord(type = FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_TYPE,
            subType = FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_UPDATE_SUB_TYPE,
            bizNo = "{{#reqVO.id}}",
            success = FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_UPDATE_SUCCESS)
    public Boolean updateReceivablePayable(FinanceReceivablePayableUpdateReqVO reqVO) {
        reqVO.setBillNo(StringUtils.trimWhitespace(reqVO.getBillNo()));
        FinanceReceivablePayableDO old = financeReceivablePayableMapper.selectById(reqVO.getId());
        if (old == null) {
            throw exception(RECEIVABLE_PAYABLE_NOT_EXISTS);
        }
        financePeriodGuardService.validateDateEditable(old.getCompanyId(), old.getBillDate());
        Long companyId = resolveCompanyId(reqVO.getCompanyId(), old.getCompanyId());
        financePeriodGuardService.validateDateEditable(companyId, reqVO.getBillDate());
        validateContactExists(reqVO.getContactId());
        validateBillNoUnique(companyId, reqVO.getBillNo(), reqVO.getId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);

        FinanceReceivablePayableDO updateDO = BeanUtils.toBean(reqVO, FinanceReceivablePayableDO.class);
        updateDO.setCompanyId(companyId);
        if (updateDO.getBalance() == null) {
            updateDO.setBalance(old.getBalance());
        }
        validateBalance(updateDO.getAmount(), updateDO.getBalance());
        updateDO.setPaidAmount(updateDO.getAmount().subtract(updateDO.getBalance()));
        if (!StringUtils.hasText(updateDO.getStatus())) {
            updateDO.setStatus(deriveStatus(updateDO.getAmount(), updateDO.getBalance()));
        }
        return SqlHelper.retBool(financeReceivablePayableMapper.updateById(updateDO));
    }

    @Override
    @LogRecord(type = FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_TYPE,
            subType = FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_DELETE_SUB_TYPE,
            bizNo = "{{#id}}",
            success = FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_DELETE_SUCCESS)
    public Boolean deleteReceivablePayable(Long id) {
        FinanceReceivablePayableDO receivablePayableDO = financeReceivablePayableMapper.selectById(id);
        if (receivablePayableDO == null) {
            return false;
        }
        financePeriodGuardService.validateDateEditable(receivablePayableDO.getCompanyId(), receivablePayableDO.getBillDate());
        if (!canDelete(receivablePayableDO)) {
            throw exception(RECEIVABLE_PAYABLE_DELETE_NOT_ALLOWED);
        }
        return SqlHelper.retBool(financeReceivablePayableMapper.deleteById(receivablePayableDO));
    }

    @Override
    public FinanceReceivablePayableDO getReceivablePayable(Long id) {
        FinanceReceivablePayableDO receivablePayableDO = financeReceivablePayableMapper.selectById(id);
        if (receivablePayableDO == null) {
            throw exception(RECEIVABLE_PAYABLE_NOT_EXISTS);
        }
        return receivablePayableDO;
    }

    @Override
    public PageResult<FinanceReceivablePayableDO> pageReceivablePayable(FinanceReceivablePayablePageReqVO reqVO) {
        return financeReceivablePayableMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_TYPE,
            subType = FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_WRITE_OFF_SUB_TYPE,
            bizNo = "{{#_ret}}",
            success = FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_WRITE_OFF_SUCCESS)
    public Long createWriteOff(FinanceReceivablePayableWriteOffSaveReqVO reqVO) {
        FinanceReceivablePayableDO receivablePayableDO = financeReceivablePayableMapper.selectByIdForUpdate(reqVO.getArpId());
        if (receivablePayableDO == null) {
            throw exception(RECEIVABLE_PAYABLE_NOT_EXISTS);
        }
        financePeriodGuardService.validateDateEditable(receivablePayableDO.getCompanyId(), reqVO.getWriteOffDate());
        validateWriteOffAmount(receivablePayableDO, reqVO.getAmount());

        Long transactionId = resolveWriteOffTransactionId(reqVO, receivablePayableDO);
        FinanceReceivablePayableDetailDO detailDO = BeanUtils.toBean(reqVO, FinanceReceivablePayableDetailDO.class)
                .setCompanyId(receivablePayableDO.getCompanyId())
                .setTransactionId(transactionId)
                .setDescription(StringUtils.trimWhitespace(reqVO.getDescription()))
                .setRemark(StringUtils.trimWhitespace(reqVO.getRemark()));
        insertWriteOffDetailWithRetry(detailDO, receivablePayableDO.getCompanyId());

        int affectedRows = financeReceivablePayableMapper.writeOffAtomically(
                receivablePayableDO.getId(),
                reqVO.getAmount(),
                FinanceReceivablePayableStatusEnum.PAID.name(),
                FinanceReceivablePayableStatusEnum.PARTIALLY_PAID.name(),
                FinanceReceivablePayableStatusEnum.CANCELLED.name());
        if (affectedRows <= 0) {
            throw exception(RECEIVABLE_PAYABLE_WRITE_OFF_CONCURRENT_CONFLICT);
        }
        return detailDO.getId();
    }

    @Override
    public PageResult<FinanceReceivablePayableDetailDO> pageWriteOff(FinanceReceivablePayableWriteOffPageReqVO reqVO) {
        return financeReceivablePayableDetailMapper.selectPage(reqVO);
    }

    private Long resolveCompanyId(Long reqCompanyId, Long fallbackCompanyId) {
        Long companyId = reqCompanyId == null ? fallbackCompanyId : reqCompanyId;
        if (companyId == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
        if (financeCompanyMapper.selectById(companyId) == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
        return companyId;
    }

    private void validateContactExists(Long contactId) {
        // 往来对象为租户级共享主数据，往来单仅校验引用对象存在，
        // 业务归属仍由 companyId 控制。
        FinanceContactDO contact = financeContactMapper.selectById(contactId);
        if (contact == null) {
            throw exception(CONTACT_NOT_EXISTS);
        }
        if (contact.getStatus() != null && !CommonStatusEnum.isEnable(contact.getStatus())) {
            throw exception(CONTACT_DISABLED);
        }
    }

    private void validateBillNoUnique(Long companyId, String billNo, Long excludeId) {
        if (!StringUtils.hasText(billNo)) {
            return;
        }
        if (financeReceivablePayableMapper.existsByBillNo(companyId, billNo, excludeId)) {
            throw exception(RECEIVABLE_PAYABLE_BILL_NO_EXISTS);
        }
    }

    private void validateBalance(BigDecimal amount, BigDecimal balance) {
        if (amount == null || balance == null) {
            throw exception(RECEIVABLE_PAYABLE_BALANCE_INVALID);
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0 || balance.compareTo(amount) > 0) {
            throw exception(RECEIVABLE_PAYABLE_BALANCE_INVALID);
        }
    }

    private void validateWriteOffAmount(FinanceReceivablePayableDO receivablePayableDO, BigDecimal writeOffAmount) {
        if (writeOffAmount == null || writeOffAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(RECEIVABLE_PAYABLE_WRITE_OFF_AMOUNT_INVALID);
        }
        if (receivablePayableDO.getBalance() == null || receivablePayableDO.getBalance().compareTo(writeOffAmount) < 0) {
            throw exception(RECEIVABLE_PAYABLE_WRITE_OFF_AMOUNT_INVALID);
        }
    }

    private Long resolveWriteOffTransactionId(FinanceReceivablePayableWriteOffSaveReqVO reqVO,
                                              FinanceReceivablePayableDO receivablePayableDO) {
        if (reqVO.getTransactionId() != null) {
            validateAssociatedTransaction(reqVO, receivablePayableDO);
            return reqVO.getTransactionId();
        }
        if (reqVO.getAccountId() == null) {
            throw exception(RECEIVABLE_PAYABLE_WRITE_OFF_ACCOUNT_REQUIRED);
        }
        return financeTransactionService.createTransaction(buildAutoTransactionReqVO(reqVO, receivablePayableDO));
    }

    private void validateAssociatedTransaction(FinanceReceivablePayableWriteOffSaveReqVO reqVO,
                                               FinanceReceivablePayableDO receivablePayableDO) {
        FinanceTransactionDO transaction = financeTransactionMapper.selectById(reqVO.getTransactionId());
        if (transaction == null) {
            throw exception(RECEIVABLE_PAYABLE_WRITE_OFF_TRANSACTION_INVALID);
        }
        if (!receivablePayableDO.getCompanyId().equals(transaction.getCompanyId())) {
            throw exception(RECEIVABLE_PAYABLE_WRITE_OFF_TRANSACTION_INVALID);
        }
        if (!FinanceTransactionStatusEnum.SUCCESS.name().equals(transaction.getStatus())) {
            throw exception(RECEIVABLE_PAYABLE_WRITE_OFF_TRANSACTION_INVALID);
        }
        financePeriodGuardService.validateDateEditable(transaction.getCompanyId(), transaction.getTransactionDate());
        FinanceTransactionTypeEnum expectedType = resolveTransactionType(receivablePayableDO.getType());
        if (!expectedType.name().equals(transaction.getTransactionType())) {
            throw exception(RECEIVABLE_PAYABLE_WRITE_OFF_TRANSACTION_INVALID);
        }
        BigDecimal usedAmount = financeReceivablePayableDetailMapper
                .selectWriteOffAmountSumByTransactionId(receivablePayableDO.getCompanyId(), transaction.getId());
        BigDecimal remainAmount = transaction.getAmount().subtract(usedAmount == null ? BigDecimal.ZERO : usedAmount);
        if (remainAmount.compareTo(reqVO.getAmount()) < 0) {
            throw exception(RECEIVABLE_PAYABLE_WRITE_OFF_TRANSACTION_AMOUNT_EXCEED);
        }
    }

    private void insertWriteOffDetailWithRetry(FinanceReceivablePayableDetailDO detailDO, Long companyId) {
        for (int i = 0; i < WRITE_OFF_NO_RETRY_TIMES; i++) {
            detailDO.setWriteOffNo(buildWriteOffNo(companyId));
            try {
                financeReceivablePayableDetailMapper.insert(detailDO);
                return;
            } catch (DuplicateKeyException duplicateKeyException) {
                if (i == WRITE_OFF_NO_RETRY_TIMES - 1) {
                    throw duplicateKeyException;
                }
            }
        }
    }

    private FinanceTransactionSaveReqVO buildAutoTransactionReqVO(FinanceReceivablePayableWriteOffSaveReqVO reqVO,
                                                                  FinanceReceivablePayableDO receivablePayableDO) {
        FinanceTransactionTypeEnum transactionType = resolveTransactionType(receivablePayableDO.getType());
        FinanceTransactionSaveReqVO transactionSaveReqVO = new FinanceTransactionSaveReqVO();
        transactionSaveReqVO.setCompanyId(receivablePayableDO.getCompanyId());
        transactionSaveReqVO.setTransactionNo(buildTransactionNo(reqVO.getTransactionNo()));
        transactionSaveReqVO.setTransactionDate(reqVO.getWriteOffDate());
        transactionSaveReqVO.setAmount(reqVO.getAmount());
        transactionSaveReqVO.setTransactionType(transactionType.name());
        transactionSaveReqVO.setAccountId(reqVO.getAccountId());
        transactionSaveReqVO.setSubjectCode(StringUtils.trimWhitespace(reqVO.getSubjectCode()));
        transactionSaveReqVO.setCategory(resolveRefType(reqVO.getCategory(), receivablePayableDO.getType()));
        transactionSaveReqVO.setDescription(resolveTransactionDescription(reqVO.getDescription(), receivablePayableDO.getBillNo()));
        transactionSaveReqVO.setStatus(FinanceTransactionStatusEnum.SUCCESS.name());
        transactionSaveReqVO.setRelatedBusinessId(receivablePayableDO.getId());
        transactionSaveReqVO.setBusinessType("EXTERNAL");
        transactionSaveReqVO.setTaxAmount(BigDecimal.ZERO);
        return transactionSaveReqVO;
    }

    private FinanceTransactionTypeEnum resolveTransactionType(String receivablePayableType) {
        if (FinanceReceivablePayableTypeEnum.RECEIVABLE.name().equals(receivablePayableType)
                || FinanceReceivablePayableTypeEnum.ADVANCE_RECEIPT.name().equals(receivablePayableType)) {
            return FinanceTransactionTypeEnum.INCOME;
        }
        if (FinanceReceivablePayableTypeEnum.PAYABLE.name().equals(receivablePayableType)
                || FinanceReceivablePayableTypeEnum.ADVANCE_PAYMENT.name().equals(receivablePayableType)) {
            return FinanceTransactionTypeEnum.EXPENSE;
        }
        throw exception(RECEIVABLE_PAYABLE_TYPE_INVALID);
    }

    private String resolveRefType(String category, String receivablePayableType) {
        if (StringUtils.hasText(category)) {
            return StringUtils.trimWhitespace(category);
        }
        if (FinanceReceivablePayableTypeEnum.RECEIVABLE.name().equals(receivablePayableType)
                || FinanceReceivablePayableTypeEnum.ADVANCE_RECEIPT.name().equals(receivablePayableType)) {
            return DEFAULT_RECEIVABLE_WRITE_OFF_REF_TYPE;
        }
        if (FinanceReceivablePayableTypeEnum.PAYABLE.name().equals(receivablePayableType)
                || FinanceReceivablePayableTypeEnum.ADVANCE_PAYMENT.name().equals(receivablePayableType)) {
            return DEFAULT_PAYABLE_WRITE_OFF_REF_TYPE;
        }
        throw exception(RECEIVABLE_PAYABLE_TYPE_INVALID);
    }

    private String resolveTransactionDescription(String description, String billNo) {
        if (StringUtils.hasText(description)) {
            return StringUtils.trimWhitespace(description);
        }
        return "往来核销[" + billNo + "]";
    }

    private String buildTransactionNo(String transactionNo) {
        if (StringUtils.hasText(transactionNo)) {
            return StringUtils.trimWhitespace(transactionNo);
        }
        LocalDateTime now = LocalDateTime.now();
        int suffix = ThreadLocalRandom.current().nextInt(100, 1000);
        return AUTO_TRANSACTION_NO_PREFIX + now.format(AUTO_NO_FORMATTER) + suffix;
    }

    private String buildWriteOffNo(Long companyId) {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 5; i++) {
            int suffix = ThreadLocalRandom.current().nextInt(100, 1000);
            String writeOffNo = AUTO_WRITE_OFF_NO_PREFIX + now.format(AUTO_NO_FORMATTER) + suffix;
            if (!financeReceivablePayableDetailMapper.existsByWriteOffNo(companyId, writeOffNo)) {
                return writeOffNo;
            }
        }
        return AUTO_WRITE_OFF_NO_PREFIX + now.format(AUTO_NO_FORMATTER) + System.nanoTime() % 100000;
    }

    private String deriveStatus(BigDecimal amount, BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            return FinanceReceivablePayableStatusEnum.PAID.name();
        }
        if (balance.compareTo(amount) < 0) {
            return FinanceReceivablePayableStatusEnum.PARTIALLY_PAID.name();
        }
        return FinanceReceivablePayableStatusEnum.UNPAID.name();
    }

    private boolean canDelete(FinanceReceivablePayableDO receivablePayableDO) {
        if (receivablePayableDO == null || receivablePayableDO.getId() == null) {
            return false;
        }
        if (financeReceivablePayableDetailMapper.existsByArpId(receivablePayableDO.getCompanyId(), receivablePayableDO.getId())) {
            return false;
        }
        BigDecimal paidAmount = receivablePayableDO.getPaidAmount() == null ? BigDecimal.ZERO : receivablePayableDO.getPaidAmount();
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            return false;
        }
        BigDecimal amount = receivablePayableDO.getAmount() == null ? BigDecimal.ZERO : receivablePayableDO.getAmount();
        BigDecimal balance = receivablePayableDO.getBalance() == null ? BigDecimal.ZERO : receivablePayableDO.getBalance();
        return FinanceReceivablePayableStatusEnum.UNPAID.name().equals(receivablePayableDO.getStatus())
                && balance.compareTo(amount) >= 0;
    }
}
