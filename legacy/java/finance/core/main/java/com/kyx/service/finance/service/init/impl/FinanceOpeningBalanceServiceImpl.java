package com.kyx.service.finance.service.init.impl;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceBatchSaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceListReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceLockReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalancePageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceRollReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceOpeningBalanceDO;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanyMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceOpeningBalanceMapper;
import com.kyx.service.finance.service.init.FinanceOpeningBalanceService;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.finance.enums.ErrorCodeConstants.COMPANY_NOT_EXISTS;
import static com.kyx.service.finance.enums.ErrorCodeConstants.OPENING_BALANCE_ROLL_PERIOD_INVALID;
import static com.kyx.service.finance.enums.ErrorCodeConstants.OPENING_BALANCE_PERIOD_LOCKED;
import static com.kyx.service.finance.enums.LogRecordConstants.*;

/**
 * 期初余额服务实现
 * <p>
 * 余额记录规则：
 * - openingAmount 正数 = 该科目余额增加（收入增加 / 资产增加 / 支出增加）
 * - openingAmount 负数 = 该科目余额减少（冲销/退款等）
 * - 无需区分借贷方向，由 subjectType 决定业务含义
 *
 * @author xyang
 */
@Service
@Validated
public class FinanceOpeningBalanceServiceImpl implements FinanceOpeningBalanceService {

    private static final int DEFAULT_STATUS = 0;
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Resource
    private FinanceOpeningBalanceMapper financeOpeningBalanceMapper;
    @Resource
    private FinanceCompanyMapper financeCompanyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FINANCE_OPENING_BALANCE_TEMPLATE_TYPE,
            subType = FINANCE_OPENING_BALANCE_TEMPLATE_BATCH_SAVE_SUB_TYPE,
            bizNo = "{{#reqVO.companyId}}",
            success = FINANCE_OPENING_BALANCE_TEMPLATE_BATCH_SAVE_SUCCESS)
    public Boolean batchSaveOpeningBalance(FinanceOpeningBalanceBatchSaveReqVO reqVO) {
        validateCompanyExists(reqVO.getCompanyId());
        validatePeriodUnlocked(reqVO.getCompanyId(), reqVO.getPeriod());

        Map<String, FinanceOpeningBalanceBatchSaveReqVO.Item> mergedItems = new LinkedHashMap<>();
        for (FinanceOpeningBalanceBatchSaveReqVO.Item item : reqVO.getItems()) {
            if (item.getSubjectCode() != null) {
                mergedItems.put(item.getSubjectCode(), item);
            }
        }

        Set<String> subjectCodes = mergedItems.keySet();
        List<FinanceOpeningBalanceDO> oldList = financeOpeningBalanceMapper
                .selectListByCompanyAndPeriodAndSubjectCodes(reqVO.getCompanyId(), reqVO.getPeriod(), subjectCodes);
        Map<String, FinanceOpeningBalanceDO> oldMap = oldList.stream()
                .collect(Collectors.toMap(FinanceOpeningBalanceDO::getSubjectCode, item -> item, (left, right) -> right));

        List<FinanceOpeningBalanceDO> upsertList = mergedItems.values().stream().map(item -> {
            FinanceOpeningBalanceDO old = oldMap.get(item.getSubjectCode());
            FinanceOpeningBalanceDO record = new FinanceOpeningBalanceDO();
            record.setCompanyId(reqVO.getCompanyId());
            record.setPeriod(reqVO.getPeriod());
            record.setSubjectCode(item.getSubjectCode());
            record.setSubjectName(item.getSubjectName());
            record.setOpeningAmount(item.getOpeningAmount() != null ? item.getOpeningAmount() : BigDecimal.ZERO);
            record.setRemark(item.getRemark());
            record.setLocked(old != null ? old.getLocked() : Boolean.FALSE);
            record.setStatus(item.getStatus() != null
                    ? item.getStatus()
                    : (old != null && old.getStatus() != null ? old.getStatus() : DEFAULT_STATUS));
            return record;
        }).collect(Collectors.toList());

        if (!upsertList.isEmpty()) {
            financeOpeningBalanceMapper.upsertBatch(upsertList);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FINANCE_OPENING_BALANCE_TEMPLATE_TYPE,
            subType = FINANCE_OPENING_BALANCE_TEMPLATE_LOCK_SUB_TYPE,
            bizNo = "{{#reqVO.companyId}}",
            success = FINANCE_OPENING_BALANCE_TEMPLATE_LOCK_SUCCESS)
    public Boolean lockOpeningBalance(FinanceOpeningBalanceLockReqVO reqVO) {
        validateCompanyExists(reqVO.getCompanyId());
        financeOpeningBalanceMapper.updateLockedByCompanyAndPeriod(reqVO.getCompanyId(), reqVO.getPeriod(), reqVO.getLocked());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FINANCE_OPENING_BALANCE_TEMPLATE_TYPE,
            subType = FINANCE_OPENING_BALANCE_TEMPLATE_ROLL_SUB_TYPE,
            bizNo = "{{#reqVO.companyId}}",
            success = FINANCE_OPENING_BALANCE_TEMPLATE_ROLL_SUCCESS)
    public Boolean rollOpeningBalance(FinanceOpeningBalanceRollReqVO reqVO) {
        validateCompanyExists(reqVO.getCompanyId());
        String fromPeriod = StringUtils.trimWhitespace(reqVO.getFromPeriod());
        String toPeriod = resolveTargetPeriod(fromPeriod, reqVO.getToPeriod());
        reqVO.setToPeriod(toPeriod);
        validateRollPeriod(fromPeriod, toPeriod);
        validatePeriodUnlocked(reqVO.getCompanyId(), toPeriod);

        List<FinanceOpeningBalanceDO> sourceList = financeOpeningBalanceMapper
                .selectListByCompanyAndPeriod(reqVO.getCompanyId(), fromPeriod);
        if (sourceList == null || sourceList.isEmpty()) {
            return true;
        }

        boolean overwrite = Boolean.TRUE.equals(reqVO.getOverwrite());
        final Map<String, FinanceOpeningBalanceDO> targetMap = overwrite
                ? Collections.emptyMap()
                : financeOpeningBalanceMapper.selectListByCompanyAndPeriod(reqVO.getCompanyId(), toPeriod)
                .stream()
                .collect(Collectors.toMap(
                        FinanceOpeningBalanceDO::getSubjectCode,
                        item -> item,
                        (left, right) -> right));

        List<FinanceOpeningBalanceDO> upsertList = sourceList.stream()
                .filter(item -> overwrite || !targetMap.containsKey(item.getSubjectCode()))
                .map(item -> new FinanceOpeningBalanceDO()
                        .setCompanyId(reqVO.getCompanyId())
                        .setPeriod(toPeriod)
                        .setSubjectCode(item.getSubjectCode())
                        .setSubjectName(item.getSubjectName())
                        .setOpeningAmount(item.getOpeningAmount() != null ? item.getOpeningAmount() : BigDecimal.ZERO)
                        .setRemark(item.getRemark())
                        .setLocked(Boolean.FALSE)
                        .setStatus(item.getStatus() == null ? DEFAULT_STATUS : item.getStatus()))
                .collect(Collectors.toList());

        if (!upsertList.isEmpty()) {
            financeOpeningBalanceMapper.upsertBatch(upsertList);
        }
        return true;
    }

    @Override
    public PageResult<FinanceOpeningBalanceDO> pageOpeningBalance(FinanceOpeningBalancePageReqVO reqVO) {
        return financeOpeningBalanceMapper.selectPage(reqVO);
    }

    @Override
    public List<FinanceOpeningBalanceDO> listOpeningBalance(FinanceOpeningBalanceListReqVO reqVO) {
        return financeOpeningBalanceMapper.selectListByReq(reqVO);
    }

    // ----------------------------------------------------------------
    // 私有方法
    // ----------------------------------------------------------------

    private void validatePeriodUnlocked(Long companyId, String period) {
        if (financeOpeningBalanceMapper.existsLockedByCompanyAndPeriod(companyId, period)) {
            throw exception(OPENING_BALANCE_PERIOD_LOCKED);
        }
    }

    private void validateCompanyExists(Long companyId) {
        if (companyId == null || financeCompanyMapper.selectById(companyId) == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
    }

    private String resolveTargetPeriod(String fromPeriod, String toPeriod) {
        if (!StringUtils.hasText(toPeriod)) {
            return parsePeriod(fromPeriod)
                    .plusMonths(1)
                    .format(PERIOD_FORMATTER);
        }
        return StringUtils.trimWhitespace(toPeriod);
    }

    private void validateRollPeriod(String fromPeriod, String toPeriod) {
        YearMonth from = parsePeriod(fromPeriod);
        YearMonth to = parsePeriod(toPeriod);
        if (!to.isAfter(from)) {
            throw exception(OPENING_BALANCE_ROLL_PERIOD_INVALID);
        }
    }

    private YearMonth parsePeriod(String period) {
        try {
            return YearMonth.parse(period, PERIOD_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw exception(OPENING_BALANCE_ROLL_PERIOD_INVALID);
        }
    }
}
