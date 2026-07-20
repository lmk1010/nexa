package com.kyx.service.finance.service.init.impl;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountPageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountSaveReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceAccountDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceAccountOptionDO;
import com.kyx.service.finance.dal.mysql.init.FinanceAccountMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceAccountOptionMapper;
import com.kyx.service.finance.dal.mysql.transaction.FinanceTransactionMapper;
import com.kyx.service.finance.dal.mysql.voucher.FinanceVoucherDetailMapper;
import com.kyx.service.finance.service.init.FinanceAccountService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.finance.enums.ErrorCodeConstants.ACCOUNT_NOT_EXISTS;
import static com.kyx.service.finance.enums.ErrorCodeConstants.ACCOUNT_OPTION_NOT_EXISTS;
import static com.kyx.service.finance.enums.ErrorCodeConstants.ACCOUNT_OPTION_VALUE_EXISTS;
import static com.kyx.service.finance.enums.ErrorCodeConstants.ACCOUNT_USED;
import static com.kyx.service.finance.enums.LogRecordConstants.FINANCE_ACCOUNT_TEMPLATE_CREATE_SUB_TYPE;
import static com.kyx.service.finance.enums.LogRecordConstants.FINANCE_ACCOUNT_TEMPLATE_CREATE_SUCCESS;
import static com.kyx.service.finance.enums.LogRecordConstants.FINANCE_ACCOUNT_TEMPLATE_DELETE_SUB_TYPE;
import static com.kyx.service.finance.enums.LogRecordConstants.FINANCE_ACCOUNT_TEMPLATE_DELETE_SUCCESS;
import static com.kyx.service.finance.enums.LogRecordConstants.FINANCE_ACCOUNT_TEMPLATE_TYPE;
import static com.kyx.service.finance.enums.LogRecordConstants.FINANCE_ACCOUNT_TEMPLATE_UPDATE_SUB_TYPE;
import static com.kyx.service.finance.enums.LogRecordConstants.FINANCE_ACCOUNT_TEMPLATE_UPDATE_SUCCESS;

/**
 * 账户服务实现类
 *
 * @author xyang
 */
@Service
@Validated
public class FinanceAccountServiceImpl implements FinanceAccountService {

    private static final String DEFAULT_CURRENCY = "CNY";
    private static final int DEFAULT_STATUS = 0;
    private static final String OPTION_TYPE_COMPANY_ENTITY = "COMPANY_ENTITY";
    private static final String OPTION_TYPE_ACCOUNT_TAG = "ACCOUNT_TAG";
    private static final String OPTION_TYPE_BANK_BRANCH = "BANK_BRANCH";

    @Resource
    private FinanceAccountMapper financeAccountMapper;
    @Resource
    private FinanceAccountOptionMapper financeAccountOptionMapper;
    @Resource
    private FinanceTransactionMapper financeTransactionMapper;
    @Resource
    private FinanceVoucherDetailMapper financeVoucherDetailMapper;

    @Override
    @LogRecord(type = FINANCE_ACCOUNT_TEMPLATE_TYPE,
            subType = FINANCE_ACCOUNT_TEMPLATE_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}",
            success = FINANCE_ACCOUNT_TEMPLATE_CREATE_SUCCESS)
    public Long createAccount(FinanceAccountSaveReqVO reqVO) {
        normalizeAccountReq(reqVO);
        FinanceAccountDO accountDO = BeanUtils.toBean(reqVO, FinanceAccountDO.class);
        accountDO.setBalance(BigDecimal.ZERO);
        setDefaultValue(accountDO);
        financeAccountMapper.insert(accountDO);
        saveAccountOptions(reqVO);
        return accountDO.getId();
    }

    @Override
    @LogRecord(type = FINANCE_ACCOUNT_TEMPLATE_TYPE,
            subType = FINANCE_ACCOUNT_TEMPLATE_UPDATE_SUB_TYPE,
            bizNo = "{{#reqVO.id}}",
            success = FINANCE_ACCOUNT_TEMPLATE_UPDATE_SUCCESS)
    public Boolean updateAccount(FinanceAccountSaveReqVO reqVO) {
        FinanceAccountDO old = financeAccountMapper.selectById(reqVO.getId());
        if (old == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        normalizeAccountReq(reqVO);
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);

        FinanceAccountDO accountDO = BeanUtils.toBean(reqVO, FinanceAccountDO.class);
        accountDO.setBalance(old.getBalance());
        setDefaultValue(accountDO);
        saveAccountOptions(reqVO);
        return SqlHelper.retBool(financeAccountMapper.updateById(accountDO));
    }

    private void setDefaultValue(FinanceAccountDO accountDO) {
        // 账户币种统一固定为 CNY，不允许外部覆盖
        accountDO.setCurrency(DEFAULT_CURRENCY);
        if (accountDO.getStatus() == null) {
            accountDO.setStatus(DEFAULT_STATUS);
        }
        if (accountDO.getReceiptFeeEnabled() == null) {
            accountDO.setReceiptFeeEnabled(Boolean.FALSE);
        }
        if (accountDO.getPaymentFeeEnabled() == null) {
            accountDO.setPaymentFeeEnabled(Boolean.FALSE);
        }
    }

    @Override
    @LogRecord(type = FINANCE_ACCOUNT_TEMPLATE_TYPE,
            subType = FINANCE_ACCOUNT_TEMPLATE_DELETE_SUB_TYPE,
            bizNo = "{{#id}}",
            success = FINANCE_ACCOUNT_TEMPLATE_DELETE_SUCCESS)
    public Boolean deleteAccount(Long id) {
        FinanceAccountDO accountDO = financeAccountMapper.selectById(id);
        if (accountDO == null) {
            return false;
        }
        if (financeTransactionMapper.existsByAccountId(id)
                || financeVoucherDetailMapper.existsByAccountId(id)) {
            throw exception(ACCOUNT_USED);
        }
        return SqlHelper.retBool(financeAccountMapper.deleteById(accountDO));
    }

    @Override
    public Boolean updateAccountStatus(Long id, Integer status) {
        FinanceAccountDO accountDO = financeAccountMapper.selectById(id);
        if (accountDO == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        accountDO.setStatus(status);
        return SqlHelper.retBool(financeAccountMapper.updateById(accountDO));
    }

    @Override
    public FinanceAccountDO getAccount(Long id) {
        FinanceAccountDO accountDO = financeAccountMapper.selectById(id);
        if (accountDO == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        return accountDO;
    }

    @Override
    public List<FinanceAccountDO> getAccountByIds(Collection<Long> ids) {
        return financeAccountMapper.selectByIds(ids);
    }

    @Override
    public PageResult<FinanceAccountDO> pageAccount(FinanceAccountPageReqVO reqVO) {
        return financeAccountMapper.selectPage(reqVO);
    }

    @Override
    public List<String> getCompanyEntityOptions() {
        return financeAccountOptionMapper.selectOptionValues(OPTION_TYPE_COMPANY_ENTITY, null);
    }

    @Override
    public List<String> getAccountTagOptions() {
        return financeAccountOptionMapper.selectOptionValues(OPTION_TYPE_ACCOUNT_TAG, null);
    }

    @Override
    public List<String> getBankBranchOptions(String keyword) {
        return financeAccountOptionMapper.selectOptionValues(OPTION_TYPE_BANK_BRANCH, keyword);
    }

    @Override
    public List<FinanceAccountOptionDO> getCompanyEntityOptionList(String keyword) {
        return financeAccountOptionMapper.selectOptions(OPTION_TYPE_COMPANY_ENTITY, keyword);
    }

    @Override
    public List<FinanceAccountOptionDO> getBankBranchOptionList(String keyword) {
        return financeAccountOptionMapper.selectOptions(OPTION_TYPE_BANK_BRANCH, keyword);
    }

    @Override
    public Long createCompanyEntityOption(String optionValue) {
        return createAccountOption(OPTION_TYPE_COMPANY_ENTITY, optionValue);
    }

    @Override
    public Boolean updateCompanyEntityOption(Long id, String optionValue) {
        return updateAccountOption(id, OPTION_TYPE_COMPANY_ENTITY, optionValue);
    }

    @Override
    public Boolean deleteCompanyEntityOption(Long id) {
        return deleteAccountOption(id, OPTION_TYPE_COMPANY_ENTITY);
    }

    @Override
    public Long createBankBranchOption(String optionValue) {
        return createAccountOption(OPTION_TYPE_BANK_BRANCH, optionValue);
    }

    @Override
    public Boolean updateBankBranchOption(Long id, String optionValue) {
        return updateAccountOption(id, OPTION_TYPE_BANK_BRANCH, optionValue);
    }

    @Override
    public Boolean deleteBankBranchOption(Long id) {
        return deleteAccountOption(id, OPTION_TYPE_BANK_BRANCH);
    }

    @Override
    public Boolean batchUpdateAccountStatus(Collection<Long> ids, Integer status) {
        List<Long> normalizedIds = normalizeIdList(ids);
        if (normalizedIds.isEmpty()) {
            return true;
        }
        return SqlHelper.retBool(financeAccountMapper.updateStatusByIds(normalizedIds, status));
    }

    @Override
    public Boolean batchDeleteAccount(Collection<Long> ids) {
        List<Long> normalizedIds = normalizeIdList(ids);
        if (normalizedIds.isEmpty()) {
            return true;
        }
        for (Long id : normalizedIds) {
            deleteAccount(id);
        }
        return true;
    }

    private void normalizeAccountReq(FinanceAccountSaveReqVO reqVO) {
        reqVO.setAccountAlias(normalizeOptionalText(reqVO.getAccountAlias()));
        reqVO.setAccountNumber(normalizeRequiredText(reqVO.getAccountNumber()));
        reqVO.setTaxNo(normalizeOptionalText(reqVO.getTaxNo()));
        reqVO.setCurrency(normalizeOptionalText(reqVO.getCurrency()));
        reqVO.setBankName(normalizeOptionalText(reqVO.getBankName()));
        reqVO.setProvinceCode(normalizeOptionalText(reqVO.getProvinceCode()));
        reqVO.setCityCode(normalizeOptionalText(reqVO.getCityCode()));
        reqVO.setDistrictCode(normalizeOptionalText(reqVO.getDistrictCode()));
        reqVO.setBranchName(normalizeOptionalText(reqVO.getBranchName()));
        reqVO.setCompanyEntity(normalizeOptionalText(reqVO.getCompanyEntity()));
        reqVO.setAccountTagText(normalizeTagText(reqVO.getAccountTagText()));
        reqVO.setRemark(normalizeOptionalText(reqVO.getRemark()));
    }

    private void saveAccountOptions(FinanceAccountSaveReqVO reqVO) {
        saveOptionIfAbsent(OPTION_TYPE_COMPANY_ENTITY, reqVO.getCompanyEntity());
        saveOptionIfAbsent(OPTION_TYPE_BANK_BRANCH, reqVO.getBranchName());
        List<String> tags = parseTags(reqVO.getAccountTagText());
        for (String tag : tags) {
            saveOptionIfAbsent(OPTION_TYPE_ACCOUNT_TAG, tag);
        }
    }

    private void saveOptionIfAbsent(String optionType, String optionValue) {
        String normalizedValue = normalizeOptionalText(optionValue);
        if (!StringUtils.hasText(normalizedValue)) {
            return;
        }
        if (financeAccountOptionMapper.existsByTypeAndValue(optionType, normalizedValue)) {
            return;
        }
        FinanceAccountOptionDO optionDO = FinanceAccountOptionDO.builder()
                .optionType(optionType)
                .optionValue(normalizedValue)
                .sort(0)
                .status(0)
                .build();
        financeAccountOptionMapper.insert(optionDO);
    }

    private Long createAccountOption(String optionType, String optionValue) {
        String normalizedValue = normalizeRequiredText(optionValue);
        validateOptionUnique(optionType, normalizedValue, null);
        FinanceAccountOptionDO optionDO = FinanceAccountOptionDO.builder()
                .optionType(optionType)
                .optionValue(normalizedValue)
                .sort(0)
                .status(0)
                .build();
        financeAccountOptionMapper.insert(optionDO);
        return optionDO.getId();
    }

    private Boolean updateAccountOption(Long id, String optionType, String optionValue) {
        FinanceAccountOptionDO optionDO = financeAccountOptionMapper.selectById(id);
        if (optionDO == null || !optionType.equals(optionDO.getOptionType())) {
            throw exception(ACCOUNT_OPTION_NOT_EXISTS);
        }
        String normalizedValue = normalizeRequiredText(optionValue);
        validateOptionUnique(optionType, normalizedValue, id);
        optionDO.setOptionValue(normalizedValue);
        return SqlHelper.retBool(financeAccountOptionMapper.updateById(optionDO));
    }

    private Boolean deleteAccountOption(Long id, String optionType) {
        FinanceAccountOptionDO optionDO = financeAccountOptionMapper.selectById(id);
        if (optionDO == null || !optionType.equals(optionDO.getOptionType())) {
            throw exception(ACCOUNT_OPTION_NOT_EXISTS);
        }
        return SqlHelper.retBool(financeAccountOptionMapper.deleteById(optionDO));
    }

    private void validateOptionUnique(String optionType, String optionValue, Long excludeId) {
        if (financeAccountOptionMapper.existsByTypeAndValue(optionType, optionValue, excludeId)) {
            throw exception(ACCOUNT_OPTION_VALUE_EXISTS);
        }
    }

    private String normalizeRequiredText(String value) {
        return StringUtils.trimWhitespace(value);
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return StringUtils.trimWhitespace(value);
    }

    private String normalizeTagText(String tagText) {
        List<String> tags = parseTags(tagText);
        if (tags.isEmpty()) {
            return null;
        }
        return String.join(",", tags);
    }

    private List<String> parseTags(String tagText) {
        if (!StringUtils.hasText(tagText)) {
            return Collections.emptyList();
        }
        return Arrays.stream(tagText.split(","))
                .map(this::normalizeOptionalText)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Long> normalizeIdList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }
}
