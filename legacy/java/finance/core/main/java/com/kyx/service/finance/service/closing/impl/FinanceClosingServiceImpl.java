package com.kyx.service.finance.service.closing.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.finance.controller.admin.closing.vo.FinanceClosingExecuteReqVO;
import com.kyx.service.finance.controller.admin.closing.vo.FinanceClosingPageReqVO;
import com.kyx.service.finance.controller.admin.closing.vo.FinanceClosingReverseReqVO;
import com.kyx.service.finance.dal.dataobject.closing.FinanceClosingDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanyDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanySubjectDO;
import com.kyx.service.finance.dal.dataobject.report.FinanceReportSnapshotDO;
import com.kyx.service.finance.dal.dataobject.voucher.FinanceVoucherDO;
import com.kyx.service.finance.dal.dataobject.voucher.FinanceVoucherDetailDO;
import com.kyx.service.finance.dal.mysql.closing.FinanceClosingMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanyMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanySubjectMapper;
import com.kyx.service.finance.dal.mysql.report.FinanceReportSnapshotMapper;
import com.kyx.service.finance.dal.mysql.transaction.FinanceTransactionMapper;
import com.kyx.service.finance.dal.mysql.voucher.FinanceVoucherMapper;
import com.kyx.service.finance.dal.mysql.voucher.FinanceVoucherDetailMapper;
import com.kyx.service.finance.enums.FinanceClosingStatusEnum;
import com.kyx.service.finance.enums.FinanceClosingTypeEnum;
import com.kyx.service.finance.enums.FinanceTransactionStatusEnum;
import com.kyx.service.finance.enums.FinanceTransactionTypeEnum;
import com.kyx.service.finance.enums.FinanceVoucherStatusEnum;
import com.kyx.service.finance.service.closing.FinanceClosingService;
import com.kyx.service.finance.service.report.dto.FinanceIncomeStatementAggregateDTO;
import com.kyx.service.finance.service.support.FinancePeriodGuardService;
import lombok.experimental.Accessors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserOperator;
import static com.kyx.service.finance.enums.ErrorCodeConstants.*;

/**
 * 月末结账 Service 实现
 *
 * @author xyang
 */
@Service
@Validated
public class FinanceClosingServiceImpl implements FinanceClosingService {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter VOUCHER_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("ddHHmmss");
    private static final String DEFAULT_CLOSING_TYPE = "MONTHLY";
    private static final String ROOT_PROFIT_SUBJECT_CODE = "4103";
    private static final String AUTO_PROFIT_TRANSFER_VOUCHER_PREFIX = "CL";
    private static final String AUTO_PROFIT_TRANSFER_VOUCHER_TYPE = "PROFIT_TRANSFER";
    private static final String AUTO_PROFIT_TRANSFER_SOURCE_TYPE = "CLOSING_PROFIT_TRANSFER";
    private static final String AUTO_PROFIT_TRANSFER_DESCRIPTION_PREFIX = "月末损益结转";
    private static final String INCOME_TYPE = FinanceTransactionTypeEnum.INCOME.name();
    private static final String EXPENSE_TYPE = FinanceTransactionTypeEnum.EXPENSE.name();
    private static final String SUCCESS_TRANSACTION_STATUS = FinanceTransactionStatusEnum.SUCCESS.name();
    private static final String UNCLASSIFIED_INCOME_CODE = "__UNCLASSIFIED_INCOME__";
    private static final String UNCLASSIFIED_EXPENSE_CODE = "__UNCLASSIFIED_EXPENSE__";
    private static final String REPORT_CODE_INCOME_STATEMENT = "INCOME_STATEMENT";
    private static final int MAX_AUTO_VOUCHER_NO_RETRY = 50;

    @Resource
    private FinanceClosingMapper financeClosingMapper;
    @Resource
    private FinanceCompanyMapper financeCompanyMapper;
    @Resource
    private FinanceCompanySubjectMapper financeCompanySubjectMapper;
    @Resource
    private FinanceTransactionMapper financeTransactionMapper;
    @Resource
    private FinanceVoucherMapper financeVoucherMapper;
    @Resource
    private FinanceVoucherDetailMapper financeVoucherDetailMapper;
    @Resource
    private FinanceReportSnapshotMapper financeReportSnapshotMapper;
    @Resource
    private FinancePeriodGuardService financePeriodGuardService;

    @Override
    public PageResult<FinanceClosingDO> pageClosing(FinanceClosingPageReqVO reqVO) {
        return financeClosingMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long close(FinanceClosingExecuteReqVO reqVO) {
        FinanceCompanyDO companyDO = validateCompany(reqVO.getCompanyId());
        String closingPeriod = normalizePeriod(reqVO.getClosingPeriod());
        validateClosingPeriodSequence(companyDO, closingPeriod);
        String closingType = normalizeClosingType(reqVO.getClosingType());
        String operator = getLoginUserOperator();

        FinanceClosingDO exist = financeClosingMapper.selectByCompanyIdAndPeriod(reqVO.getCompanyId(), closingPeriod);
        FinanceClosingStatusEnum oldStatus = null;
        if (exist != null) {
            oldStatus = parseStatusOrThrow(exist.getStatus());
            if (FinanceClosingStatusEnum.SUCCESS.equals(oldStatus)) {
                throw exception(CLOSING_ALREADY_SUCCESS);
            }
        }

        ClosingPrecheckSnapshot precheckSnapshot = precheckBeforeClose(reqVO.getCompanyId(), closingPeriod);
        ProfitTransferVoucherSnapshot profitTransferSnapshot = createProfitTransferVoucher(
                reqVO.getCompanyId(), closingPeriod, operator, reqVO.getRemark());
        saveIncomeStatementSnapshot(reqVO.getCompanyId(), closingPeriod);
        String precheckResult = buildPrecheckResult(precheckSnapshot, profitTransferSnapshot);
        LocalDateTime closeTime = LocalDateTime.now();

        if (exist != null) {
            FinanceClosingDO updateDO = new FinanceClosingDO()
                    .setId(exist.getId())
                    .setClosingType(closingType)
                    .setStatus(FinanceClosingStatusEnum.SUCCESS.name())
                    .setPrecheckResult(precheckResult)
                    .setProfitTransferVoucherId(profitTransferSnapshot.voucherId)
                    .setCloseTime(closeTime)
                    .setClosedBy(operator)
                    .setReversedTime(null)
                    .setReversedBy(null)
                    .setRemark(reqVO.getRemark());
            boolean updated = financeClosingMapper.update(updateDO, new LambdaUpdateWrapper<FinanceClosingDO>()
                    .eq(FinanceClosingDO::getId, exist.getId())
                    .eq(FinanceClosingDO::getStatus, oldStatus.name())) > 0;
            if (!updated) {
                FinanceClosingDO latest = financeClosingMapper.selectById(exist.getId());
                if (latest != null && FinanceClosingStatusEnum.SUCCESS.name().equals(latest.getStatus())) {
                    throw exception(CLOSING_ALREADY_SUCCESS);
                }
                throw exception(CLOSING_CONCURRENT_CONFLICT);
            }
            updateCompanyClosePeriod(reqVO.getCompanyId(), closingPeriod);
            financePeriodGuardService.lockPeriod(reqVO.getCompanyId(), closingPeriod, operator, reqVO.getRemark());
            return exist.getId();
        }

        FinanceClosingDO closingDO = new FinanceClosingDO()
                .setCompanyId(reqVO.getCompanyId())
                .setClosingPeriod(closingPeriod)
                .setClosingType(closingType)
                .setStatus(FinanceClosingStatusEnum.SUCCESS.name())
                .setPrecheckResult(precheckResult)
                .setProfitTransferVoucherId(profitTransferSnapshot.voucherId)
                .setCloseTime(closeTime)
                .setClosedBy(operator)
                .setRemark(reqVO.getRemark());
        try {
            financeClosingMapper.insert(closingDO);
        } catch (DuplicateKeyException ignore) {
            FinanceClosingDO duplicate = financeClosingMapper.selectByCompanyIdAndPeriod(reqVO.getCompanyId(), closingPeriod);
            if (duplicate == null) {
                throw ignore;
            }
            FinanceClosingStatusEnum duplicateStatus = parseStatusOrThrow(duplicate.getStatus());
            if (FinanceClosingStatusEnum.SUCCESS.equals(duplicateStatus)) {
                throw exception(CLOSING_ALREADY_SUCCESS);
            }
            FinanceClosingDO retryUpdate = new FinanceClosingDO()
                    .setId(duplicate.getId())
                    .setClosingType(closingType)
                    .setStatus(FinanceClosingStatusEnum.SUCCESS.name())
                    .setPrecheckResult(precheckResult)
                    .setProfitTransferVoucherId(profitTransferSnapshot.voucherId)
                    .setCloseTime(closeTime)
                    .setClosedBy(operator)
                    .setReversedTime(null)
                    .setReversedBy(null)
                    .setRemark(reqVO.getRemark());
            boolean retried = financeClosingMapper.update(retryUpdate, new LambdaUpdateWrapper<FinanceClosingDO>()
                    .eq(FinanceClosingDO::getId, duplicate.getId())
                    .eq(FinanceClosingDO::getStatus, duplicateStatus.name())) > 0;
            if (!retried) {
                FinanceClosingDO latest = financeClosingMapper.selectById(duplicate.getId());
                if (latest != null && FinanceClosingStatusEnum.SUCCESS.name().equals(latest.getStatus())) {
                    throw exception(CLOSING_ALREADY_SUCCESS);
                }
                throw exception(CLOSING_CONCURRENT_CONFLICT);
            }
            updateCompanyClosePeriod(reqVO.getCompanyId(), closingPeriod);
            financePeriodGuardService.lockPeriod(reqVO.getCompanyId(), closingPeriod, operator, reqVO.getRemark());
            return duplicate.getId();
        }
        updateCompanyClosePeriod(reqVO.getCompanyId(), closingPeriod);
        financePeriodGuardService.lockPeriod(reqVO.getCompanyId(), closingPeriod, operator, reqVO.getRemark());
        return closingDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reverse(FinanceClosingReverseReqVO reqVO) {
        String operator = getLoginUserOperator();
        FinanceClosingDO closingDO = financeClosingMapper.selectById(reqVO.getId());
        if (closingDO == null) {
            throw exception(CLOSING_NOT_EXISTS);
        }
        validateCompany(closingDO.getCompanyId());
        closingDO = financeClosingMapper.selectById(reqVO.getId());
        if (closingDO == null) {
            throw exception(CLOSING_NOT_EXISTS);
        }
        FinanceClosingStatusEnum oldStatus = parseStatusOrThrow(closingDO.getStatus());
        if (!FinanceClosingStatusEnum.SUCCESS.equals(oldStatus)) {
            throw exception(CLOSING_REVERSE_ONLY_SUCCESS);
        }
        validateReverseSequence(closingDO);
        FinanceClosingDO updateDO = new FinanceClosingDO()
                .setId(closingDO.getId())
                .setStatus(FinanceClosingStatusEnum.REVERSED.name())
                .setReversedTime(LocalDateTime.now())
                .setReversedBy(operator)
                .setRemark(reqVO.getRemark());
        boolean updated = financeClosingMapper.update(updateDO, new LambdaUpdateWrapper<FinanceClosingDO>()
                .eq(FinanceClosingDO::getId, closingDO.getId())
                .eq(FinanceClosingDO::getStatus, FinanceClosingStatusEnum.SUCCESS.name())) > 0;
        if (!updated) {
            throw exception(CLOSING_CONCURRENT_CONFLICT);
        }
        reverseProfitTransferVoucherIfNeeded(closingDO, reqVO.getRemark());
        deleteIncomeStatementSnapshot(closingDO.getCompanyId(), closingDO.getClosingPeriod());
        financePeriodGuardService.unlockPeriod(closingDO.getCompanyId(), closingDO.getClosingPeriod(), operator, reqVO.getRemark());
        refreshCompanyClosePeriodAfterReverse(closingDO.getCompanyId());
        return true;
    }

    private FinanceCompanyDO validateCompany(Long companyId) {
        FinanceCompanyDO companyDO = companyId == null ? null : financeCompanyMapper.selectById(companyId);
        if (companyDO == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
        return companyDO;
    }

    private String normalizePeriod(String closingPeriod) {
        if (!StringUtils.hasText(closingPeriod)) {
            throw exception(CLOSING_PERIOD_INVALID);
        }
        String normalized = StringUtils.trimWhitespace(closingPeriod);
        try {
            return YearMonth.parse(normalized, PERIOD_FORMATTER).format(PERIOD_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw exception(CLOSING_PERIOD_INVALID);
        }
    }

    private String normalizeClosingType(String closingType) {
        if (!StringUtils.hasText(closingType)) {
            return DEFAULT_CLOSING_TYPE;
        }
        String normalized = StringUtils.trimWhitespace(closingType).toUpperCase(Locale.ROOT);
        try {
            return FinanceClosingTypeEnum.valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            throw exception(CLOSING_TYPE_INVALID);
        }
    }

    private FinanceClosingStatusEnum parseStatusOrThrow(String status) {
        if (!StringUtils.hasText(status)) {
            throw exception(CLOSING_STATUS_INVALID);
        }
        try {
            return FinanceClosingStatusEnum.valueOf(StringUtils.trimWhitespace(status).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw exception(CLOSING_STATUS_INVALID);
        }
    }

    private void updateCompanyClosePeriod(Long companyId, String closingPeriod) {
        FinanceCompanyDO updateDO = new FinanceCompanyDO()
                .setId(companyId)
                .setCurrentClosePeriod(closingPeriod);
        SqlHelper.retBool(financeCompanyMapper.updateById(updateDO));
    }

    private ClosingPrecheckSnapshot precheckBeforeClose(Long companyId, String closingPeriod) {
        int unpostedVoucherCount = financeVoucherMapper.countUnpostedByCompanyIdAndPeriod(companyId, closingPeriod);
        if (unpostedVoucherCount > 0) {
            throw exception(CLOSING_PRECHECK_UNPOSTED_VOUCHER);
        }
        int draftTransactionCount = financeTransactionMapper.countDraftByCompanyIdAndPeriod(companyId, closingPeriod);
        if (draftTransactionCount > 0) {
            throw exception(CLOSING_PRECHECK_DRAFT_TRANSACTION);
        }
        return new ClosingPrecheckSnapshot(unpostedVoucherCount, draftTransactionCount);
    }

    private ProfitTransferVoucherSnapshot createProfitTransferVoucher(Long companyId,
                                                                      String closingPeriod,
                                                                      String operator,
                                                                      String remark) {
        List<FinanceIncomeStatementAggregateDTO> aggregateList = loadIncomeStatementAggregateByPeriod(companyId, closingPeriod);
        if (aggregateList == null || aggregateList.isEmpty()) {
            return ProfitTransferVoucherSnapshot.notGenerated();
        }

        Map<String, String> subjectNameMap = buildEnabledSubjectNameMap(companyId);
        String profitSubjectName = subjectNameMap.get(ROOT_PROFIT_SUBJECT_CODE);
        if (!StringUtils.hasText(profitSubjectName)) {
            throw exception(CLOSING_PROFIT_TRANSFER_SUBJECT_INVALID);
        }

        List<TransferEntryLine> entryLines = buildTransferEntryLines(aggregateList, subjectNameMap, profitSubjectName);
        if (entryLines.isEmpty()) {
            return ProfitTransferVoucherSnapshot.notGenerated();
        }

        BigDecimal totalDebit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCredit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (TransferEntryLine line : entryLines) {
            totalDebit = totalDebit.add(line.debitAmount);
            totalCredit = totalCredit.add(line.creditAmount);
        }
        if (totalDebit.compareTo(BigDecimal.ZERO) <= 0 || totalDebit.compareTo(totalCredit) != 0) {
            throw exception(CLOSING_PROFIT_TRANSFER_BALANCE_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime voucherDate = resolveVoucherDateTime(closingPeriod);
        String description = buildVoucherDescription(closingPeriod, remark);
        for (int retry = 0; retry < MAX_AUTO_VOUCHER_NO_RETRY; retry++) {
            String voucherNo = buildAutoVoucherNo(closingPeriod, retry);
            if (financeVoucherMapper.existsByVoucherNo(companyId, voucherNo, null)) {
                continue;
            }
            try {
                FinanceVoucherDO voucherDO = new FinanceVoucherDO()
                        .setCompanyId(companyId)
                        .setVoucherNo(voucherNo)
                        .setVoucherDate(voucherDate)
                        .setVoucherPeriod(closingPeriod)
                        .setVoucherType(AUTO_PROFIT_TRANSFER_VOUCHER_TYPE)
                        .setStatus(FinanceVoucherStatusEnum.POSTED.name())
                        .setTotalDebit(totalDebit)
                        .setTotalCredit(totalCredit)
                        .setSourceType(AUTO_PROFIT_TRANSFER_SOURCE_TYPE)
                        .setSourceNo(closingPeriod)
                        .setDescription(description)
                        .setPostedBy(operator)
                        .setPostedTime(now);
                financeVoucherMapper.insert(voucherDO);
                saveProfitTransferDetails(voucherDO.getId(), companyId, entryLines, description);
                return ProfitTransferVoucherSnapshot.generated(voucherDO.getId(), voucherNo);
            } catch (DuplicateKeyException ignore) {
                // 并发场景下凭证号唯一键冲突，继续重试。
            }
        }
        throw exception(CLOSING_PROFIT_TRANSFER_VOUCHER_NO_CONFLICT);
    }

    private List<TransferEntryLine> buildTransferEntryLines(List<FinanceIncomeStatementAggregateDTO> aggregateList,
                                                            Map<String, String> subjectNameMap,
                                                            String profitSubjectName) {
        List<FinanceIncomeStatementAggregateDTO> sortedList = new ArrayList<>(aggregateList);
        sortedList.sort(Comparator.comparing(item -> item == null || item.getSubjectCode() == null
                ? ""
                : StringUtils.trimWhitespace(item.getSubjectCode())));

        List<TransferEntryLine> lines = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalExpense = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (FinanceIncomeStatementAggregateDTO item : sortedList) {
            if (item == null || !StringUtils.hasText(item.getSubjectCode())) {
                continue;
            }
            String subjectCode = StringUtils.trimWhitespace(item.getSubjectCode());
            BigDecimal incomeAmount = normalizeAmount(item.getIncomeAmount());
            BigDecimal expenseAmount = normalizeAmount(item.getExpenseAmount());
            if (incomeAmount.compareTo(BigDecimal.ZERO) > 0) {
                String subjectName = resolveTransferSubjectName(subjectCode, subjectNameMap);
                lines.add(new TransferEntryLine(subjectCode, subjectName, incomeAmount, BigDecimal.ZERO));
                totalIncome = totalIncome.add(incomeAmount);
            }
            if (expenseAmount.compareTo(BigDecimal.ZERO) > 0) {
                String subjectName = resolveTransferSubjectName(subjectCode, subjectNameMap);
                lines.add(new TransferEntryLine(subjectCode, subjectName, BigDecimal.ZERO, expenseAmount));
                totalExpense = totalExpense.add(expenseAmount);
            }
        }

        if (totalIncome.compareTo(BigDecimal.ZERO) <= 0 && totalExpense.compareTo(BigDecimal.ZERO) <= 0) {
            return new ArrayList<>();
        }

        BigDecimal netProfit = totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_UP);
        if (netProfit.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new TransferEntryLine(
                    ROOT_PROFIT_SUBJECT_CODE,
                    profitSubjectName,
                    BigDecimal.ZERO,
                    netProfit));
        } else if (netProfit.compareTo(BigDecimal.ZERO) < 0) {
            lines.add(new TransferEntryLine(
                    ROOT_PROFIT_SUBJECT_CODE,
                    profitSubjectName,
                    netProfit.abs(),
                    BigDecimal.ZERO));
        }
        return lines;
    }

    private String resolveTransferSubjectName(String subjectCode, Map<String, String> subjectNameMap) {
        if (!StringUtils.hasText(subjectCode)
                || UNCLASSIFIED_INCOME_CODE.equals(subjectCode)
                || UNCLASSIFIED_EXPENSE_CODE.equals(subjectCode)) {
            throw exception(CLOSING_PROFIT_TRANSFER_SUBJECT_INVALID);
        }
        String subjectName = subjectNameMap.get(StringUtils.trimWhitespace(subjectCode));
        if (!StringUtils.hasText(subjectName)) {
            throw exception(CLOSING_PROFIT_TRANSFER_SUBJECT_INVALID);
        }
        return subjectName;
    }

    private Map<String, String> buildEnabledSubjectNameMap(Long companyId) {
        List<FinanceCompanySubjectDO> subjectList = financeCompanySubjectMapper.selectListByCompanyId(companyId);
        Map<String, String> subjectNameMap = new HashMap<>();
        if (subjectList == null || subjectList.isEmpty()) {
            return subjectNameMap;
        }
        for (FinanceCompanySubjectDO subject : subjectList) {
            if (subject == null || !StringUtils.hasText(subject.getSubjectCode()) || !StringUtils.hasText(subject.getSubjectName())) {
                continue;
            }
            Integer status = subject.getStatus();
            if (status != null && !CommonStatusEnum.isEnable(status)) {
                continue;
            }
            String subjectCode = StringUtils.trimWhitespace(subject.getSubjectCode());
            subjectNameMap.putIfAbsent(subjectCode, StringUtils.trimWhitespace(subject.getSubjectName()));
        }
        return subjectNameMap;
    }

    private LocalDateTime resolveVoucherDateTime(String closingPeriod) {
        YearMonth yearMonth = YearMonth.parse(closingPeriod, PERIOD_FORMATTER);
        return yearMonth.atEndOfMonth().atTime(23, 59, 59);
    }

    private void saveProfitTransferDetails(Long voucherId,
                                           Long companyId,
                                           List<TransferEntryLine> entryLines,
                                           String description) {
        List<FinanceVoucherDetailDO> detailList = new ArrayList<>(entryLines.size());
        for (int i = 0; i < entryLines.size(); i++) {
            TransferEntryLine line = entryLines.get(i);
            FinanceVoucherDetailDO detailDO = new FinanceVoucherDetailDO()
                    .setCompanyId(companyId)
                    .setVoucherId(voucherId)
                    .setLineNo(i + 1)
                    .setSubjectCode(line.subjectCode)
                    .setSubjectName(line.subjectName)
                    .setDebitAmount(line.debitAmount)
                    .setCreditAmount(line.creditAmount)
                    .setTaxAmount(BigDecimal.ZERO)
                    .setDescription(description);
            detailList.add(detailDO);
        }
        financeVoucherDetailMapper.insertBatch(detailList);
    }

    private String buildPrecheckResult(ClosingPrecheckSnapshot precheckSnapshot,
                                       ProfitTransferVoucherSnapshot profitTransferSnapshot) {
        String transferMessage = profitTransferSnapshot.generated
                ? "已生成损益结转凭证[" + profitTransferSnapshot.voucherNo + "]"
                : "本期无可结转损益，未生成凭证";
        String message = "检查通过：未过账凭证="
                + precheckSnapshot.unpostedVoucherCount
                + "，草稿流水="
                + precheckSnapshot.draftTransactionCount
                + "；"
                + transferMessage;

        StringBuilder json = new StringBuilder(256);
        json.append("{\"result\":\"PASS\"");
        json.append(",\"message\":\"").append(escapeJson(message)).append("\"");
        json.append(",\"checks\":{\"unpostedVoucherCount\":")
                .append(precheckSnapshot.unpostedVoucherCount)
                .append(",\"draftTransactionCount\":")
                .append(precheckSnapshot.draftTransactionCount)
                .append("}");
        json.append(",\"profitTransfer\":{\"generated\":").append(profitTransferSnapshot.generated);
        if (profitTransferSnapshot.voucherId != null) {
            json.append(",\"voucherId\":").append(profitTransferSnapshot.voucherId);
        }
        if (StringUtils.hasText(profitTransferSnapshot.voucherNo)) {
            json.append(",\"voucherNo\":\"").append(escapeJson(profitTransferSnapshot.voucherNo)).append("\"");
        }
        json.append("}}");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String buildVoucherDescription(String closingPeriod, String remark) {
        String base = AUTO_PROFIT_TRANSFER_DESCRIPTION_PREFIX + "(" + closingPeriod + ")";
        if (!StringUtils.hasText(remark)) {
            return base;
        }
        return base + " " + StringUtils.trimWhitespace(remark);
    }

    private String buildAutoVoucherNo(String closingPeriod, int retry) {
        String retrySuffix = String.format(Locale.ROOT, "%02d", retry);
        return AUTO_PROFIT_TRANSFER_VOUCHER_PREFIX
                + closingPeriod
                + LocalDateTime.now().format(VOUCHER_NO_TIME_FORMATTER)
                + retrySuffix;
    }

    private void saveIncomeStatementSnapshot(Long companyId, String closingPeriod) {
        List<FinanceIncomeStatementAggregateDTO> aggregateList = loadIncomeStatementAggregateByPeriod(companyId, closingPeriod);
        IncomeStatementSnapshotPayload payload = new IncomeStatementSnapshotPayload()
                .setAggregateList(aggregateList == null ? new ArrayList<>() : aggregateList);
        LocalDate snapshotDate = YearMonth.parse(closingPeriod, PERIOD_FORMATTER).atEndOfMonth();
        String dataJson = JsonUtils.toJsonString(payload);

        FinanceReportSnapshotDO existing = financeReportSnapshotMapper.selectByCompanyIdAndReportCodeAndPeriod(
                companyId, REPORT_CODE_INCOME_STATEMENT, closingPeriod);
        if (existing == null) {
            try {
                financeReportSnapshotMapper.insert(new FinanceReportSnapshotDO()
                        .setCompanyId(companyId)
                        .setReportCode(REPORT_CODE_INCOME_STATEMENT)
                        .setSnapshotPeriod(closingPeriod)
                        .setSnapshotDate(snapshotDate)
                        .setDataJson(dataJson));
                return;
            } catch (DuplicateKeyException ignore) {
                existing = financeReportSnapshotMapper.selectByCompanyIdAndReportCodeAndPeriod(
                        companyId, REPORT_CODE_INCOME_STATEMENT, closingPeriod);
            }
        }
        if (existing == null) {
            return;
        }
        financeReportSnapshotMapper.updateById(new FinanceReportSnapshotDO()
                .setId(existing.getId())
                .setSnapshotDate(snapshotDate)
                .setDataJson(dataJson));
    }

    private void deleteIncomeStatementSnapshot(Long companyId, String closingPeriod) {
        financeReportSnapshotMapper.deleteByCompanyIdAndReportCodeAndPeriod(
                companyId, REPORT_CODE_INCOME_STATEMENT, closingPeriod);
    }

    private List<FinanceIncomeStatementAggregateDTO> loadIncomeStatementAggregateByPeriod(Long companyId, String closingPeriod) {
        return financeTransactionMapper.selectIncomeStatementAggregateByPeriodRange(
                companyId,
                closingPeriod,
                closingPeriod,
                SUCCESS_TRANSACTION_STATUS,
                INCOME_TYPE,
                EXPENSE_TYPE,
                UNCLASSIFIED_INCOME_CODE,
                UNCLASSIFIED_EXPENSE_CODE);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private void reverseProfitTransferVoucherIfNeeded(FinanceClosingDO closingDO, String remark) {
        if (closingDO == null || closingDO.getProfitTransferVoucherId() == null) {
            return;
        }
        Long voucherId = closingDO.getProfitTransferVoucherId();
        FinanceVoucherDO voucherDO = financeVoucherMapper.selectById(voucherId);
        if (voucherDO == null || voucherDO.getCompanyId() == null || !voucherDO.getCompanyId().equals(closingDO.getCompanyId())) {
            return;
        }
        FinanceVoucherStatusEnum oldStatus = parseVoucherStatusOrThrow(voucherDO.getStatus());
        if (FinanceVoucherStatusEnum.VOID.equals(oldStatus)) {
            return;
        }

        FinanceVoucherDO updateDO = new FinanceVoucherDO()
                .setId(voucherId)
                .setStatus(FinanceVoucherStatusEnum.VOID.name())
                .setDescription(buildReversedVoucherDescription(voucherDO.getDescription(), remark));
        boolean updated = financeVoucherMapper.update(updateDO, new LambdaUpdateWrapper<FinanceVoucherDO>()
                .eq(FinanceVoucherDO::getId, voucherId)
                .eq(FinanceVoucherDO::getCompanyId, closingDO.getCompanyId())
                .eq(FinanceVoucherDO::getStatus, oldStatus.name())) > 0;
        if (updated) {
            return;
        }
        FinanceVoucherDO latest = financeVoucherMapper.selectById(voucherId);
        if (latest != null && FinanceVoucherStatusEnum.VOID.name().equals(latest.getStatus())) {
            return;
        }
        throw exception(CLOSING_CONCURRENT_CONFLICT);
    }

    private FinanceVoucherStatusEnum parseVoucherStatusOrThrow(String status) {
        if (!StringUtils.hasText(status)) {
            throw exception(VOUCHER_STATUS_INVALID);
        }
        try {
            return FinanceVoucherStatusEnum.valueOf(StringUtils.trimWhitespace(status).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw exception(VOUCHER_STATUS_INVALID);
        }
    }

    private String buildReversedVoucherDescription(String originDescription, String remark) {
        String reversedMark = " [反结账自动作废]";
        String base = StringUtils.hasText(originDescription)
                ? StringUtils.trimWhitespace(originDescription)
                : AUTO_PROFIT_TRANSFER_DESCRIPTION_PREFIX;
        if (base.contains(reversedMark)) {
            return base;
        }
        if (!StringUtils.hasText(remark)) {
            return base + reversedMark;
        }
        return base + reversedMark + "(" + StringUtils.trimWhitespace(remark) + ")";
    }

    private void refreshCompanyClosePeriodAfterReverse(Long companyId) {
        FinanceClosingDO latestSuccess = financeClosingMapper.selectLatestSuccessByCompanyId(companyId);
        if (latestSuccess != null) {
            updateCompanyClosePeriod(companyId, latestSuccess.getClosingPeriod());
            return;
        }
        FinanceCompanyDO companyDO = financeCompanyMapper.selectById(companyId);
        if (companyDO == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
        String fallbackPeriod = StringUtils.hasText(companyDO.getStartPeriod())
                ? companyDO.getStartPeriod()
                : companyDO.getCurrentClosePeriod();
        if (!StringUtils.hasText(fallbackPeriod)) {
            throw exception(CLOSING_PERIOD_INVALID);
        }
        updateCompanyClosePeriod(companyId, normalizePeriod(fallbackPeriod));
    }

    private void validateClosingPeriodSequence(FinanceCompanyDO companyDO, String closingPeriod) {
        if (companyDO == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
        String basePeriod = StringUtils.hasText(companyDO.getCurrentClosePeriod())
                ? companyDO.getCurrentClosePeriod()
                : companyDO.getStartPeriod();
        if (!StringUtils.hasText(basePeriod)) {
            throw exception(CLOSING_PERIOD_INVALID);
        }
        YearMonth base = YearMonth.parse(normalizePeriod(basePeriod), PERIOD_FORMATTER);
        YearMonth target = YearMonth.parse(closingPeriod, PERIOD_FORMATTER);
        if (target.isBefore(base) || target.isAfter(base.plusMonths(1))) {
            throw exception(CLOSING_PERIOD_SEQUENCE_INVALID);
        }
    }

    private void validateReverseSequence(FinanceClosingDO closingDO) {
        FinanceClosingDO latestSuccess = financeClosingMapper.selectLatestSuccessByCompanyId(closingDO.getCompanyId());
        if (latestSuccess == null || latestSuccess.getId() == null
                || !latestSuccess.getId().equals(closingDO.getId())) {
            throw exception(CLOSING_REVERSE_SEQUENCE_INVALID);
        }
    }

    private static final class ClosingPrecheckSnapshot {
        private final int unpostedVoucherCount;
        private final int draftTransactionCount;

        private ClosingPrecheckSnapshot(int unpostedVoucherCount, int draftTransactionCount) {
            this.unpostedVoucherCount = unpostedVoucherCount;
            this.draftTransactionCount = draftTransactionCount;
        }
    }

    private static final class ProfitTransferVoucherSnapshot {
        private final boolean generated;
        private final Long voucherId;
        private final String voucherNo;

        private ProfitTransferVoucherSnapshot(boolean generated, Long voucherId, String voucherNo) {
            this.generated = generated;
            this.voucherId = voucherId;
            this.voucherNo = voucherNo;
        }

        private static ProfitTransferVoucherSnapshot notGenerated() {
            return new ProfitTransferVoucherSnapshot(false, null, null);
        }

        private static ProfitTransferVoucherSnapshot generated(Long voucherId, String voucherNo) {
            return new ProfitTransferVoucherSnapshot(true, voucherId, voucherNo);
        }
    }

    private static final class TransferEntryLine {
        private final String subjectCode;
        private final String subjectName;
        private final BigDecimal debitAmount;
        private final BigDecimal creditAmount;

        private TransferEntryLine(String subjectCode, String subjectName, BigDecimal debitAmount, BigDecimal creditAmount) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.debitAmount = debitAmount == null ? BigDecimal.ZERO : debitAmount.setScale(2, RoundingMode.HALF_UP);
            this.creditAmount = creditAmount == null ? BigDecimal.ZERO : creditAmount.setScale(2, RoundingMode.HALF_UP);
        }
    }

    @lombok.Data
    @Accessors(chain = true)
    private static final class IncomeStatementSnapshotPayload {
        private List<FinanceIncomeStatementAggregateDTO> aggregateList = new ArrayList<>();
    }
}
