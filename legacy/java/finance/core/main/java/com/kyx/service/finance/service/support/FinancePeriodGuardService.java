package com.kyx.service.finance.service.support;

import com.kyx.service.finance.dal.dataobject.closing.FinancePeriodLockDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanyDO;
import com.kyx.service.finance.dal.mysql.closing.FinancePeriodLockMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanyMapper;
import com.kyx.service.finance.enums.FinancePeriodLockStatusEnum;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.finance.enums.ErrorCodeConstants.*;

/**
 * 财务期间校验服务
 * @author xyang
 */
@Service
@Validated
public class FinancePeriodGuardService {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Resource
    private FinanceCompanyMapper financeCompanyMapper;
    @Resource
    private FinancePeriodLockMapper financePeriodLockMapper;

    public void validateDateEditable(Long companyId, LocalDateTime businessDate) {
        if (businessDate == null) {
            throw exception(CLOSING_PERIOD_INVALID);
        }
        validatePeriodEditable(companyId, businessDate.format(PERIOD_FORMATTER));
    }

    public void validatePeriodEditable(Long companyId, String period) {
        FinanceCompanyDO companyDO = validateCompany(companyId);
        String normalizedPeriod = normalizePeriod(period);
        validateByCompanyClosedPeriod(companyDO, normalizedPeriod);
        if (financePeriodLockMapper.existsLocked(companyId, normalizedPeriod)) {
            throw exception(PERIOD_LOCKED_NOT_ALLOWED);
        }
    }

    public void lockPeriod(Long companyId, String period, String operator, String reason) {
        upsertPeriodLock(companyId, period, operator, reason, true);
    }

    public void unlockPeriod(Long companyId, String period, String operator, String reason) {
        upsertPeriodLock(companyId, period, operator, reason, false);
    }

    private void validateByCompanyClosedPeriod(FinanceCompanyDO companyDO, String normalizedPeriod) {
        if (companyDO == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
        if (!StringUtils.hasText(companyDO.getCurrentClosePeriod())) {
            return;
        }
        YearMonth closedPeriod = YearMonth.parse(normalizePeriod(companyDO.getCurrentClosePeriod()), PERIOD_FORMATTER);
        YearMonth targetPeriod = YearMonth.parse(normalizedPeriod, PERIOD_FORMATTER);
        if (!targetPeriod.isAfter(closedPeriod)) {
            throw exception(PERIOD_LOCKED_NOT_ALLOWED);
        }
    }

    private FinanceCompanyDO validateCompany(Long companyId) {
        FinanceCompanyDO companyDO = companyId == null ? null : financeCompanyMapper.selectById(companyId);
        if (companyDO == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
        return companyDO;
    }

    private String normalizePeriod(String period) {
        if (!StringUtils.hasText(period)) {
            throw exception(CLOSING_PERIOD_INVALID);
        }
        String normalized = StringUtils.trimWhitespace(period);
        try {
            return YearMonth.parse(normalized, PERIOD_FORMATTER).format(PERIOD_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw exception(CLOSING_PERIOD_INVALID);
        }
    }

    private void upsertPeriodLock(Long companyId, String period, String operator, String reason, boolean lock) {
        validateCompany(companyId);
        String normalizedPeriod = normalizePeriod(period);
        LocalDateTime now = LocalDateTime.now();
        FinancePeriodLockDO exist = financePeriodLockMapper.selectByCompanyIdAndPeriod(companyId, normalizedPeriod);
        if (exist == null) {
            FinancePeriodLockDO insertDO = new FinancePeriodLockDO()
                    .setCompanyId(companyId)
                    .setPeriod(normalizedPeriod)
                    .setLockStatus(FinancePeriodLockStatusEnum.lockStatus(lock).name())
                    .setLockReason(StringUtils.trimWhitespace(reason))
                    .setLockedBy(lock ? operator : null)
                    .setLockedTime(lock ? now : null)
                    .setUnlockedBy(lock ? null : operator)
                    .setUnlockedTime(lock ? null : now);
            try {
                financePeriodLockMapper.insert(insertDO);
                return;
            } catch (DuplicateKeyException ignore) {
                exist = financePeriodLockMapper.selectByCompanyIdAndPeriod(companyId, normalizedPeriod);
                if (exist == null) {
                    throw ignore;
                }
            }
        }
        FinancePeriodLockDO updateDO = new FinancePeriodLockDO()
                .setId(exist.getId())
                .setLockStatus(FinancePeriodLockStatusEnum.lockStatus(lock).name())
                .setLockReason(StringUtils.trimWhitespace(reason))
                .setLockedBy(lock ? operator : exist.getLockedBy())
                .setLockedTime(lock ? now : exist.getLockedTime())
                .setUnlockedBy(lock ? null : operator)
                .setUnlockedTime(lock ? null : now);
        financePeriodLockMapper.updateById(updateDO);
    }
}
