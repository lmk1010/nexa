package com.kyx.service.finance.service.report.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.service.finance.controller.admin.report.vo.FinanceAmountComparisonItemRespVO;
import com.kyx.service.finance.controller.admin.report.vo.FinanceAssetStatementRespVO;
import com.kyx.service.finance.controller.admin.report.vo.FinanceCashflowStatementRespVO;
import com.kyx.service.finance.controller.admin.report.vo.FinanceIncomeStatementReqVO;
import com.kyx.service.finance.controller.admin.report.vo.FinanceIncomeStatementRespVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanyDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanySubjectDO;
import com.kyx.service.finance.dal.dataobject.report.FinanceReportSnapshotDO;
import com.kyx.service.finance.dal.mysql.init.FinanceAccountMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanyMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanySubjectMapper;
import com.kyx.service.finance.dal.mysql.receivable.FinanceReceivablePayableMapper;
import com.kyx.service.finance.dal.mysql.report.FinanceReportSnapshotMapper;
import com.kyx.service.finance.dal.mysql.transaction.FinanceTransactionMapper;
import com.kyx.service.finance.enums.FinanceReceivablePayableStatusEnum;
import com.kyx.service.finance.enums.FinanceReceivablePayableTypeEnum;
import com.kyx.service.finance.enums.FinanceTransactionStatusEnum;
import com.kyx.service.finance.enums.FinanceTransactionTypeEnum;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardArpBalanceSummaryDTO;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardCashFlowActivitySummaryDTO;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardTransactionSummaryDTO;
import com.kyx.service.finance.service.report.FinanceReportService;
import com.kyx.service.finance.service.report.dto.FinanceIncomeStatementAggregateDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.finance.enums.ErrorCodeConstants.CLOSING_PERIOD_INVALID;
import static com.kyx.service.finance.enums.ErrorCodeConstants.COMPANY_NOT_EXISTS;

/**
 * 财务报表 Service 实现
 *
 * @author xyang
 */
@Service
@Validated
@Slf4j
public class FinanceReportServiceImpl implements FinanceReportService {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String ROOT_SUBJECT_CODE = "0";
    private static final String TOTAL_ITEM_CODE = "TOTAL";
    private static final String TOTAL_ITEM_NAME = "合计";
    private static final String UNCLASSIFIED_INCOME_CODE = "__UNCLASSIFIED_INCOME__";
    private static final String UNCLASSIFIED_EXPENSE_CODE = "__UNCLASSIFIED_EXPENSE__";
    private static final String UNCLASSIFIED_INCOME_NAME = "未分类收入";
    private static final String UNCLASSIFIED_EXPENSE_NAME = "未分类支出";
    private static final String SUBJECT_TYPE_INCOME = FinanceTransactionTypeEnum.INCOME.name();
    private static final String SUBJECT_TYPE_EXPENSE = FinanceTransactionTypeEnum.EXPENSE.name();
    private static final String SUBJECT_TYPE_COST = "COST";
    private static final String TRANSACTION_STATUS_SUCCESS = FinanceTransactionStatusEnum.SUCCESS.name();
    private static final String RECEIVABLE_TYPE = FinanceReceivablePayableTypeEnum.RECEIVABLE.name();
    private static final String PAYABLE_TYPE = FinanceReceivablePayableTypeEnum.PAYABLE.name();
    private static final String RECEIVABLE_PAYABLE_CANCELLED_STATUS = FinanceReceivablePayableStatusEnum.CANCELLED.name();
    private static final String REPORT_CODE_INCOME_STATEMENT = "INCOME_STATEMENT";

    @Resource
    private FinanceTransactionMapper financeTransactionMapper;
    @Resource
    private FinanceReceivablePayableMapper financeReceivablePayableMapper;
    @Resource
    private FinanceAccountMapper financeAccountMapper;
    @Resource
    private FinanceCompanyMapper financeCompanyMapper;
    @Resource
    private FinanceCompanySubjectMapper financeCompanySubjectMapper;
    @Resource
    private FinanceReportSnapshotMapper financeReportSnapshotMapper;

    @Override
    public List<FinanceIncomeStatementRespVO> getIncomeStatement(FinanceIncomeStatementReqVO reqVO) {
        validateCompany(reqVO.getCompanyId());

        PeriodRange periodRange = resolvePeriodRange(reqVO.getPeriod());
        List<FinanceCompanySubjectDO> subjects = financeCompanySubjectMapper.selectListByCompanyId(reqVO.getCompanyId())
                .stream()
                .filter(item -> SUBJECT_TYPE_INCOME.equals(item.getSubjectType())
                        || SUBJECT_TYPE_EXPENSE.equals(item.getSubjectType())
                        || SUBJECT_TYPE_COST.equals(item.getSubjectType()))
                .collect(Collectors.toList());

        List<FinanceIncomeStatementAggregateDTO> aggregateList = loadIncomeStatementAggregate(reqVO.getCompanyId(), periodRange);
        Map<String, AmountBundle> aggregateMap = toAggregateMap(aggregateList);

        List<FinanceIncomeStatementRespVO> roots = buildTreeRows(subjects, aggregateMap);
        for (FinanceIncomeStatementRespVO root : roots) {
            rollup(root);
        }

        AmountBundle totalBundle = toTotalAmount(aggregateList);
        List<FinanceIncomeStatementRespVO> result = new ArrayList<>(roots.size() + 1);
        result.addAll(roots);
        result.add(buildRow(TOTAL_ITEM_CODE, TOTAL_ITEM_NAME, totalBundle.income, totalBundle.expense));
        return result;
    }

    private List<FinanceIncomeStatementAggregateDTO> loadIncomeStatementAggregate(Long companyId, PeriodRange periodRange) {
        if (periodRange != null && Objects.equals(periodRange.startPeriod, periodRange.endPeriod)) {
            FinanceReportSnapshotDO snapshotDO = financeReportSnapshotMapper.selectByCompanyIdAndReportCodeAndPeriod(
                    companyId, REPORT_CODE_INCOME_STATEMENT, periodRange.startPeriod);
            if (snapshotDO != null && StringUtils.hasText(snapshotDO.getDataJson())) {
                List<FinanceIncomeStatementAggregateDTO> snapshotAggregateList =
                        parseIncomeStatementAggregateSnapshot(snapshotDO.getDataJson());
                if (snapshotAggregateList != null) {
                    return snapshotAggregateList;
                }
            }
        }
        return financeTransactionMapper.selectIncomeStatementAggregateByPeriodRange(
                companyId,
                periodRange.startPeriod,
                periodRange.endPeriod,
                TRANSACTION_STATUS_SUCCESS,
                SUBJECT_TYPE_INCOME,
                SUBJECT_TYPE_EXPENSE,
                UNCLASSIFIED_INCOME_CODE,
                UNCLASSIFIED_EXPENSE_CODE);
    }

    private List<FinanceIncomeStatementAggregateDTO> parseIncomeStatementAggregateSnapshot(String dataJson) {
        try {
            IncomeStatementSnapshotPayload payload = JsonUtils.parseObject(
                    dataJson, new TypeReference<IncomeStatementSnapshotPayload>() {
                    });
            if (payload != null && payload.getAggregateList() != null) {
                return payload.getAggregateList();
            }
            return JsonUtils.parseObject(dataJson, new TypeReference<List<FinanceIncomeStatementAggregateDTO>>() {
            });
        } catch (RuntimeException ex) {
            log.warn("利润表快照解析失败，回退实时口径。dataJson={}", dataJson, ex);
            return null;
        }
    }

    @Override
    public FinanceAssetStatementRespVO getAssetStatement(FinanceIncomeStatementReqVO reqVO) {
        validateCompany(reqVO.getCompanyId());
        String period = resolveMonthPeriod(reqVO.getPeriod());
        String previousPeriod = resolvePreviousPeriod(period);

        MonthlyMetrics currentMetrics = buildMonthlyMetrics(reqVO.getCompanyId(), period);
        MonthlyMetrics previousMetrics = buildMonthlyMetrics(reqVO.getCompanyId(), previousPeriod);
        BigDecimal accountBalanceAmount = buildAccountBalanceByPeriod(reqVO.getCompanyId(), period);

        BigDecimal assetAmount = normalizeAmount(accountBalanceAmount.add(currentMetrics.receivableBalance));
        BigDecimal liabilityAmount = normalizeAmount(currentMetrics.payableBalance);
        BigDecimal equityAmount = normalizeAmount(assetAmount.subtract(liabilityAmount));

        BigDecimal previousAccountBalanceAmount = buildAccountBalanceByPeriod(reqVO.getCompanyId(), previousPeriod);
        BigDecimal previousAssetAmount = normalizeAmount(previousAccountBalanceAmount.add(previousMetrics.receivableBalance));
        BigDecimal previousLiabilityAmount = normalizeAmount(previousMetrics.payableBalance);
        BigDecimal previousEquityAmount = normalizeAmount(previousAssetAmount.subtract(previousLiabilityAmount));

        List<FinanceAmountComparisonItemRespVO> rows = new ArrayList<>();
        rows.add(buildComparisonRow("ASSET", "资产", assetAmount, previousAssetAmount));
        rows.add(buildComparisonRow("LIABILITY", "负债", liabilityAmount, previousLiabilityAmount));
        rows.add(buildComparisonRow("EQUITY", "所有者权益", equityAmount, previousEquityAmount));
        rows.add(buildComparisonRow("ACCOUNT_BALANCE", "其中：账户余额", accountBalanceAmount, previousAccountBalanceAmount));
        rows.add(buildComparisonRow("RECEIVABLE_BALANCE", "其中：应收余额", currentMetrics.receivableBalance, previousMetrics.receivableBalance));
        rows.add(buildComparisonRow("PAYABLE_BALANCE", "其中：应付余额", currentMetrics.payableBalance, previousMetrics.payableBalance));
        rows.add(buildComparisonRow(TOTAL_ITEM_CODE, TOTAL_ITEM_NAME, assetAmount, previousAssetAmount));

        return new FinanceAssetStatementRespVO()
                .setPeriod(period)
                .setPreviousPeriod(previousPeriod)
                .setAccountBalanceAmount(accountBalanceAmount)
                .setReceivableBalanceAmount(currentMetrics.receivableBalance)
                .setPayableBalanceAmount(currentMetrics.payableBalance)
                .setAssetAmount(assetAmount)
                .setLiabilityAmount(liabilityAmount)
                .setEquityAmount(equityAmount)
                .setRows(rows);
    }

    @Override
    public FinanceCashflowStatementRespVO getCashflowStatement(FinanceIncomeStatementReqVO reqVO) {
        validateCompany(reqVO.getCompanyId());
        String period = resolveMonthPeriod(reqVO.getPeriod());
        String previousPeriod = resolvePreviousPeriod(period);

        MonthlyMetrics currentMetrics = buildMonthlyMetrics(reqVO.getCompanyId(), period);
        MonthlyMetrics previousMetrics = buildMonthlyMetrics(reqVO.getCompanyId(), previousPeriod);
        BigDecimal endingCashAmount = buildAccountBalanceByPeriod(reqVO.getCompanyId(), period);
        BigDecimal previousEndingCashAmount = buildAccountBalanceByPeriod(reqVO.getCompanyId(), previousPeriod);

        FinanceDashboardCashFlowActivitySummaryDTO currentSummary = financeTransactionMapper.selectDashboardCashFlowActivitySummary(
                reqVO.getCompanyId(), period, TRANSACTION_STATUS_SUCCESS, SUBJECT_TYPE_INCOME, SUBJECT_TYPE_EXPENSE);
        FinanceDashboardCashFlowActivitySummaryDTO previousSummary = financeTransactionMapper.selectDashboardCashFlowActivitySummary(
                reqVO.getCompanyId(), previousPeriod, TRANSACTION_STATUS_SUCCESS, SUBJECT_TYPE_INCOME, SUBJECT_TYPE_EXPENSE);

        BigDecimal operatingCashFlowAmount = normalizeAmount(currentSummary == null ? null : currentSummary.getOperatingNetCash());
        BigDecimal investingCashFlowAmount = normalizeAmount(currentSummary == null ? null : currentSummary.getInvestingNetCash());
        BigDecimal financingCashFlowAmount = normalizeAmount(currentSummary == null ? null : currentSummary.getFinancingNetCash());
        BigDecimal previousOperatingCashFlowAmount = normalizeAmount(previousSummary == null ? null : previousSummary.getOperatingNetCash());
        BigDecimal previousInvestingCashFlowAmount = normalizeAmount(previousSummary == null ? null : previousSummary.getInvestingNetCash());
        BigDecimal previousFinancingCashFlowAmount = normalizeAmount(previousSummary == null ? null : previousSummary.getFinancingNetCash());

        List<FinanceAmountComparisonItemRespVO> rows = new ArrayList<>();
        rows.add(buildComparisonRow("ENDING_CASH", "期末现金余额", endingCashAmount, previousEndingCashAmount));
        rows.add(buildComparisonRow("NET_CASH_INCREASE", "净现金增加额", currentMetrics.netAmount, previousMetrics.netAmount));
        rows.add(buildComparisonRow("OPERATING_CASH_FLOW", "经营活动现金流", operatingCashFlowAmount, previousOperatingCashFlowAmount));
        rows.add(buildComparisonRow("INVESTING_CASH_FLOW", "投资活动现金流", investingCashFlowAmount, previousInvestingCashFlowAmount));
        rows.add(buildComparisonRow("FINANCING_CASH_FLOW", "筹资活动现金流", financingCashFlowAmount, previousFinancingCashFlowAmount));
        rows.add(buildComparisonRow(
                TOTAL_ITEM_CODE,
                TOTAL_ITEM_NAME,
                operatingCashFlowAmount.add(investingCashFlowAmount).add(financingCashFlowAmount),
                previousOperatingCashFlowAmount.add(previousInvestingCashFlowAmount).add(previousFinancingCashFlowAmount)));

        return new FinanceCashflowStatementRespVO()
                .setPeriod(period)
                .setPreviousPeriod(previousPeriod)
                .setEndingCashAmount(endingCashAmount)
                .setNetCashIncreaseAmount(currentMetrics.netAmount)
                .setOperatingCashFlowAmount(operatingCashFlowAmount)
                .setInvestingCashFlowAmount(investingCashFlowAmount)
                .setFinancingCashFlowAmount(financingCashFlowAmount)
                .setRows(rows);
    }

    private List<FinanceIncomeStatementRespVO> buildTreeRows(List<FinanceCompanySubjectDO> subjects,
                                                             Map<String, AmountBundle> aggregateMap) {
        List<FinanceCompanySubjectDO> sortedSubjects = (subjects == null ? new ArrayList<FinanceCompanySubjectDO>() : subjects).stream()
                .sorted(Comparator.comparing(
                                (FinanceCompanySubjectDO item) -> item.getLevel() == null ? Integer.MAX_VALUE : item.getLevel())
                        .thenComparing(item -> item.getSubjectCode() == null ? "" : item.getSubjectCode())
                        .thenComparing(item -> item.getId() == null ? Long.MAX_VALUE : item.getId()))
                .collect(Collectors.toList());

        Map<String, FinanceIncomeStatementRespVO> nodeMap = new HashMap<>(sortedSubjects.size());
        for (FinanceCompanySubjectDO subject : sortedSubjects) {
            AmountBundle amountBundle = aggregateMap.getOrDefault(subject.getSubjectCode(), new AmountBundle());
            FinanceIncomeStatementRespVO row = buildRow(subject.getSubjectCode(), subject.getSubjectName(),
                    amountBundle.income, amountBundle.expense);
            nodeMap.put(subject.getSubjectCode(), row);
        }

        List<FinanceIncomeStatementRespVO> orphans = new ArrayList<>();
        for (Map.Entry<String, AmountBundle> entry : aggregateMap.entrySet()) {
            String subjectCode = entry.getKey();
            if (!StringUtils.hasText(subjectCode) || nodeMap.containsKey(subjectCode)) {
                continue;
            }
            AmountBundle amountBundle = entry.getValue();
            FinanceIncomeStatementRespVO orphanRow = buildRow(subjectCode, resolveOrphanName(subjectCode),
                    amountBundle.income, amountBundle.expense);
            nodeMap.put(subjectCode, orphanRow);
            orphans.add(orphanRow);
        }

        List<FinanceIncomeStatementRespVO> roots = new ArrayList<>();
        for (FinanceCompanySubjectDO subject : sortedSubjects) {
            FinanceIncomeStatementRespVO current = nodeMap.get(subject.getSubjectCode());
            if (current == null) {
                continue;
            }
            String parentCode = StringUtils.trimWhitespace(subject.getParentCode());
            FinanceIncomeStatementRespVO parent = StringUtils.hasText(parentCode) ? nodeMap.get(parentCode) : null;
            if (ROOT_SUBJECT_CODE.equals(parentCode) || parent == null || Objects.equals(parentCode, subject.getSubjectCode())) {
                roots.add(current);
                continue;
            }
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(current);
        }
        roots.addAll(orphans);
        return roots;
    }

    private String resolveOrphanName(String subjectCode) {
        if (UNCLASSIFIED_INCOME_CODE.equals(subjectCode)) {
            return UNCLASSIFIED_INCOME_NAME;
        }
        if (UNCLASSIFIED_EXPENSE_CODE.equals(subjectCode)) {
            return UNCLASSIFIED_EXPENSE_NAME;
        }
        return "未知科目(" + subjectCode + ")";
    }

    private AmountBundle rollup(FinanceIncomeStatementRespVO node) {
        AmountBundle bundle = new AmountBundle();
        bundle.income = normalizeAmount(node.getCurrentIncome());
        bundle.expense = normalizeAmount(node.getCurrentExpense());
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (FinanceIncomeStatementRespVO child : node.getChildren()) {
                AmountBundle childBundle = rollup(child);
                bundle.income = bundle.income.add(childBundle.income);
                bundle.expense = bundle.expense.add(childBundle.expense);
            }
        }
        node.setCurrentIncome(normalizeAmount(bundle.income));
        node.setCurrentExpense(normalizeAmount(bundle.expense));
        return bundle;
    }

    private FinanceIncomeStatementRespVO buildRow(String itemCode, String itemName,
                                                  BigDecimal currentIncome, BigDecimal currentExpense) {
        return new FinanceIncomeStatementRespVO()
                .setItemCode(itemCode)
                .setItemName(itemName)
                .setCurrentIncome(normalizeAmount(currentIncome))
                .setCurrentExpense(normalizeAmount(currentExpense));
    }

    private Map<String, AmountBundle> toAggregateMap(List<FinanceIncomeStatementAggregateDTO> list) {
        Map<String, AmountBundle> map = new HashMap<>();
        if (list == null || list.isEmpty()) {
            return map;
        }
        for (FinanceIncomeStatementAggregateDTO item : list) {
            if (item == null || !StringUtils.hasText(item.getSubjectCode())) {
                continue;
            }
            String subjectCode = StringUtils.trimWhitespace(item.getSubjectCode());
            AmountBundle bundle = map.computeIfAbsent(subjectCode, key -> new AmountBundle());
            bundle.income = bundle.income.add(normalizeAmount(item.getIncomeAmount()));
            bundle.expense = bundle.expense.add(normalizeAmount(item.getExpenseAmount()));
        }
        return map;
    }

    private AmountBundle toTotalAmount(List<FinanceIncomeStatementAggregateDTO> list) {
        AmountBundle bundle = new AmountBundle();
        if (list == null || list.isEmpty()) {
            return bundle;
        }
        for (FinanceIncomeStatementAggregateDTO item : list) {
            if (item == null) {
                continue;
            }
            bundle.income = bundle.income.add(normalizeAmount(item.getIncomeAmount()));
            bundle.expense = bundle.expense.add(normalizeAmount(item.getExpenseAmount()));
        }
        return bundle;
    }

    private FinanceAmountComparisonItemRespVO buildComparisonRow(String itemCode,
                                                                 String itemName,
                                                                 BigDecimal currentAmount,
                                                                 BigDecimal previousAmount) {
        BigDecimal current = normalizeAmount(currentAmount);
        BigDecimal previous = normalizeAmount(previousAmount);
        return new FinanceAmountComparisonItemRespVO()
                .setItemCode(itemCode)
                .setItemName(itemName)
                .setCurrentAmount(current)
                .setPreviousAmount(previous)
                .setChangeRate(calculateChangeRate(current, previous));
    }

    private MonthlyMetrics buildMonthlyMetrics(Long companyId, String period) {
        FinanceDashboardTransactionSummaryDTO transactionSummary = financeTransactionMapper.selectDashboardTransactionSummary(
                companyId,
                period,
                TRANSACTION_STATUS_SUCCESS,
                SUBJECT_TYPE_INCOME,
                SUBJECT_TYPE_EXPENSE);
        PeriodDateRange periodDateRange = buildPeriodDateRange(period);
        FinanceDashboardArpBalanceSummaryDTO arpSummary = financeReceivablePayableMapper.selectDashboardArpBalanceSummaryByBillDateRange(
                companyId,
                RECEIVABLE_TYPE,
                PAYABLE_TYPE,
                RECEIVABLE_PAYABLE_CANCELLED_STATUS,
                periodDateRange.startTime,
                periodDateRange.endTime);

        BigDecimal incomeAmount = normalizeAmount(transactionSummary == null ? null : transactionSummary.getIncomeAmount());
        BigDecimal expenseAmount = normalizeAmount(transactionSummary == null ? null : transactionSummary.getExpenseAmount());
        BigDecimal receivableBalance = normalizeAmount(arpSummary == null ? null : arpSummary.getReceivableBalance());
        BigDecimal payableBalance = normalizeAmount(arpSummary == null ? null : arpSummary.getPayableBalance());

        MonthlyMetrics metrics = new MonthlyMetrics();
        metrics.incomeAmount = incomeAmount;
        metrics.expenseAmount = expenseAmount;
        metrics.netAmount = normalizeAmount(incomeAmount.subtract(expenseAmount));
        metrics.receivableBalance = receivableBalance;
        metrics.payableBalance = payableBalance;
        return metrics;
    }

    private BigDecimal buildAccountBalanceByPeriod(Long companyId, String period) {
        return normalizeAmount(financeAccountMapper.selectDashboardAccountBalanceByCompanyUsageScopeAndPeriod(
                companyId, period, TRANSACTION_STATUS_SUCCESS, SUBJECT_TYPE_INCOME, SUBJECT_TYPE_EXPENSE));
    }

    private String resolveMonthPeriod(String period) {
        if (!StringUtils.hasText(period)) {
            return YearMonth.now().format(PERIOD_FORMATTER);
        }
        String normalized = StringUtils.trimWhitespace(period);
        try {
            return YearMonth.parse(normalized, PERIOD_FORMATTER).format(PERIOD_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw exception(CLOSING_PERIOD_INVALID);
        }
    }

    private String resolvePreviousPeriod(String period) {
        return YearMonth.parse(period, PERIOD_FORMATTER)
                .minusMonths(1)
                .format(PERIOD_FORMATTER);
    }

    private PeriodDateRange buildPeriodDateRange(String period) {
        YearMonth yearMonth = YearMonth.parse(period, PERIOD_FORMATTER);
        return new PeriodDateRange(
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.plusMonths(1).atDay(1).atStartOfDay());
    }

    private BigDecimal calculateChangeRate(BigDecimal currentAmount, BigDecimal previousAmount) {
        BigDecimal current = normalizeAmount(currentAmount);
        BigDecimal previous = normalizeAmount(previousAmount);
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 2, RoundingMode.HALF_UP);
    }

    private void validateCompany(Long companyId) {
        FinanceCompanyDO companyDO = companyId == null ? null : financeCompanyMapper.selectById(companyId);
        if (companyDO == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
    }

    private PeriodRange resolvePeriodRange(String period) {
        YearMonth now = YearMonth.now();
        if (!StringUtils.hasText(period)) {
            String endPeriod = now.format(PERIOD_FORMATTER);
            String startPeriod = now.getYear() + "01";
            return new PeriodRange(startPeriod, endPeriod);
        }
        String normalized = StringUtils.trimWhitespace(period);
        try {
            String normalizedPeriod = YearMonth.parse(normalized, PERIOD_FORMATTER).format(PERIOD_FORMATTER);
            return new PeriodRange(normalizedPeriod, normalizedPeriod);
        } catch (DateTimeParseException ex) {
            throw exception(CLOSING_PERIOD_INVALID);
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class AmountBundle {
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;
    }

    private static final class MonthlyMetrics {
        private BigDecimal incomeAmount = BigDecimal.ZERO;
        private BigDecimal expenseAmount = BigDecimal.ZERO;
        private BigDecimal netAmount = BigDecimal.ZERO;
        private BigDecimal receivableBalance = BigDecimal.ZERO;
        private BigDecimal payableBalance = BigDecimal.ZERO;
    }

    private static final class PeriodRange {
        private final String startPeriod;
        private final String endPeriod;

        private PeriodRange(String startPeriod, String endPeriod) {
            this.startPeriod = startPeriod;
            this.endPeriod = endPeriod;
        }
    }

    private static final class PeriodDateRange {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        private PeriodDateRange(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    @Data
    private static final class IncomeStatementSnapshotPayload {
        private List<FinanceIncomeStatementAggregateDTO> aggregateList = new ArrayList<>();
    }
}
