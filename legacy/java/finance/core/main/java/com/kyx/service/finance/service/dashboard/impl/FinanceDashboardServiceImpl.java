package com.kyx.service.finance.service.dashboard.impl;

import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardMetricsReqVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardMetricsRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardOverviewRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardCashflowRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardAssetRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardOperatingRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardReportArpContactLineRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardReportArpRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardReportChannelBalanceRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardReportRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardWeeklyCashFlowRespVO;
import com.kyx.service.finance.controller.admin.dashboard.vo.FinanceDashboardAggregateRespVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanyDO;
import com.kyx.service.finance.dal.mysql.init.FinanceAccountMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanyMapper;
import com.kyx.service.finance.dal.mysql.receivable.FinanceReceivablePayableMapper;
import com.kyx.service.finance.dal.mysql.transaction.FinanceTransactionMapper;
import com.kyx.service.finance.enums.FinanceReceivablePayableStatusEnum;
import com.kyx.service.finance.enums.FinanceReceivablePayableTypeEnum;
import com.kyx.service.finance.enums.FinanceTransactionStatusEnum;
import com.kyx.service.finance.enums.FinanceTransactionTypeEnum;
import com.kyx.service.finance.service.dashboard.FinanceDashboardService;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardArpBalanceSummaryDTO;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardArpContactSummaryDTO;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardCashFlowActivitySummaryDTO;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardChannelBalanceSummaryDTO;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardTransactionSummaryDTO;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardWeeklyCashFlowDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.finance.enums.ErrorCodeConstants.CLOSING_PERIOD_INVALID;
import static com.kyx.service.finance.enums.ErrorCodeConstants.COMPANY_NOT_EXISTS;

/**
 * 经营看板 Service 实现
 */
@Service
@Validated
public class FinanceDashboardServiceImpl implements FinanceDashboardService {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter WEEK_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Integer REPORT_ARP_CONTACT_LIMIT = 3;
    private static final int FOUR_WEEK_COUNT = 4;
    private static final String TRANSACTION_STATUS_SUCCESS = FinanceTransactionStatusEnum.SUCCESS.name();
    private static final String TRANSACTION_TYPE_INCOME = FinanceTransactionTypeEnum.INCOME.name();
    private static final String TRANSACTION_TYPE_EXPENSE = FinanceTransactionTypeEnum.EXPENSE.name();
    private static final String TRANSACTION_TYPE_TRANSFER = FinanceTransactionTypeEnum.TRANSFER.name();
    private static final String ACCOUNT_TYPE_WECHAT = "WECHAT";
    private static final String ACCOUNT_TYPE_ALIPAY = "ALIPAY";
    private static final String ACCOUNT_TYPE_BANK = "BANK";

    @Resource
    private FinanceTransactionMapper financeTransactionMapper;
    @Resource
    private FinanceReceivablePayableMapper financeReceivablePayableMapper;
    @Resource
    private FinanceAccountMapper financeAccountMapper;
    @Resource
    private FinanceCompanyMapper financeCompanyMapper;

    @Override
    public FinanceDashboardMetricsRespVO getMetrics(FinanceDashboardMetricsReqVO reqVO) {
        FinanceDashboardOverviewRespVO overview = getOverview(reqVO);
        return new FinanceDashboardMetricsRespVO()
                .setPeriod(overview.getPeriod())
                .setIncomeAmount(overview.getOperatingIncomeAmount())
                .setExpenseAmount(overview.getOperatingExpenseAmount())
                .setNetAmount(overview.getOperatingProfitAmount())
                .setReceivableBalance(overview.getReceivableBalanceAmount())
                .setPayableBalance(overview.getPayableBalanceAmount());
    }

    @Override
    public FinanceDashboardOperatingRespVO getOperatingModule(FinanceDashboardMetricsReqVO reqVO) {
        validateCompany(reqVO.getCompanyId());
        String period = resolvePeriod(reqVO.getPeriod());
        String previousPeriod = resolvePreviousPeriod(period);
        FinanceDashboardMetricsRespVO currentMetrics = buildMetrics(reqVO.getCompanyId(), period);
        FinanceDashboardMetricsRespVO previousMetrics = buildMetrics(reqVO.getCompanyId(), previousPeriod);

        return new FinanceDashboardOperatingRespVO()
                .setPeriod(period)
                .setPreviousPeriod(previousPeriod)
                .setOperatingIncomeAmount(currentMetrics.getIncomeAmount())
                .setPreviousOperatingIncomeAmount(previousMetrics.getIncomeAmount())
                .setOperatingIncomeChangeRate(calculateChangeRate(
                        currentMetrics.getIncomeAmount(),
                        previousMetrics.getIncomeAmount()))
                .setOperatingExpenseAmount(currentMetrics.getExpenseAmount())
                .setPreviousOperatingExpenseAmount(previousMetrics.getExpenseAmount())
                .setOperatingExpenseChangeRate(calculateChangeRate(
                        currentMetrics.getExpenseAmount(),
                        previousMetrics.getExpenseAmount()))
                .setOperatingProfitAmount(currentMetrics.getNetAmount())
                .setPreviousOperatingProfitAmount(previousMetrics.getNetAmount())
                .setOperatingProfitChangeRate(calculateChangeRate(
                        currentMetrics.getNetAmount(),
                        previousMetrics.getNetAmount()));
    }

    @Override
    public FinanceDashboardReportRespVO getReportModule(FinanceDashboardMetricsReqVO reqVO) {
        validateCompany(reqVO.getCompanyId());
        String period = resolvePeriod(reqVO.getPeriod());
        String previousPeriod = resolvePreviousPeriod(period);
        FinanceDashboardMetricsRespVO currentMetrics = buildMetrics(reqVO.getCompanyId(), period);
        FinanceDashboardMetricsRespVO previousMetrics = buildMetrics(reqVO.getCompanyId(), previousPeriod);
        FourWeekCashFlowResult currentFourWeekCashFlow = buildFourWeekCashFlowResult(reqVO.getCompanyId(), period);
        FourWeekCashFlowResult previousFourWeekCashFlow = buildFourWeekCashFlowResult(reqVO.getCompanyId(), previousPeriod);

        BigDecimal accountBalance = buildAccountBalanceByPeriod(reqVO.getCompanyId(), period);
        BigDecimal previousAccountBalance = buildAccountBalanceByPeriod(reqVO.getCompanyId(), previousPeriod);
        List<FinanceDashboardReportChannelBalanceRespVO> channelBalanceLines = buildReportChannelBalanceLines(
                reqVO.getCompanyId(), period, previousPeriod);

        return new FinanceDashboardReportRespVO()
                .setPeriod(period)
                .setPreviousPeriod(previousPeriod)
                .setFourWeekCashFlowAmount(currentFourWeekCashFlow.getTotalNetCashFlowAmount())
                .setPreviousFourWeekCashFlowAmount(previousFourWeekCashFlow.getTotalNetCashFlowAmount())
                .setAccountBalanceAmount(accountBalance)
                .setPreviousAccountBalanceAmount(previousAccountBalance)
                .setAccountBalanceChangeRate(calculateChangeRate(accountBalance, previousAccountBalance))
                .setReceivableBalanceAmount(currentMetrics.getReceivableBalance())
                .setPreviousReceivableBalanceAmount(previousMetrics.getReceivableBalance())
                .setPayableBalanceAmount(currentMetrics.getPayableBalance())
                .setPreviousPayableBalanceAmount(previousMetrics.getPayableBalance())
                .setChannelBalanceLines(channelBalanceLines);
    }

    @Override
    public FinanceDashboardReportArpRespVO getReportArpModule(FinanceDashboardMetricsReqVO reqVO) {
        validateCompany(reqVO.getCompanyId());
        String period = resolvePeriod(reqVO.getPeriod());
        String previousPeriod = resolvePreviousPeriod(period);
        PeriodDateRange currentRange = buildPeriodDateRange(period);
        PeriodDateRange previousRange = buildPeriodDateRange(previousPeriod);

        FinanceDashboardArpBalanceSummaryDTO currentSummary = financeReceivablePayableMapper.selectDashboardArpBalanceSummaryByBillDateRange(
                reqVO.getCompanyId(),
                FinanceReceivablePayableTypeEnum.RECEIVABLE.name(),
                FinanceReceivablePayableTypeEnum.PAYABLE.name(),
                FinanceReceivablePayableStatusEnum.CANCELLED.name(),
                currentRange.getStartTime(),
                currentRange.getEndTime());
        FinanceDashboardArpBalanceSummaryDTO previousSummary = financeReceivablePayableMapper.selectDashboardArpBalanceSummaryByBillDateRange(
                reqVO.getCompanyId(),
                FinanceReceivablePayableTypeEnum.RECEIVABLE.name(),
                FinanceReceivablePayableTypeEnum.PAYABLE.name(),
                FinanceReceivablePayableStatusEnum.CANCELLED.name(),
                previousRange.getStartTime(),
                previousRange.getEndTime());
        BigDecimal receivableBalanceAmount = normalizeAmount(currentSummary == null ? null : currentSummary.getReceivableBalance());
        BigDecimal previousReceivableBalanceAmount = normalizeAmount(previousSummary == null ? null : previousSummary.getReceivableBalance());
        BigDecimal payableBalanceAmount = normalizeAmount(currentSummary == null ? null : currentSummary.getPayableBalance());
        BigDecimal previousPayableBalanceAmount = normalizeAmount(previousSummary == null ? null : previousSummary.getPayableBalance());
        BigDecimal advanceReceiptAmount = normalizeAmount(
                financeReceivablePayableMapper.selectDashboardAdvanceReceiptByBillDateRange(
                        reqVO.getCompanyId(),
                        FinanceReceivablePayableTypeEnum.ADVANCE_RECEIPT.name(),
                        FinanceReceivablePayableStatusEnum.CANCELLED.name(),
                        currentRange.getStartTime(),
                        currentRange.getEndTime()));
        BigDecimal previousAdvanceReceiptAmount = normalizeAmount(
                financeReceivablePayableMapper.selectDashboardAdvanceReceiptByBillDateRange(
                        reqVO.getCompanyId(),
                        FinanceReceivablePayableTypeEnum.ADVANCE_RECEIPT.name(),
                        FinanceReceivablePayableStatusEnum.CANCELLED.name(),
                        previousRange.getStartTime(),
                        previousRange.getEndTime()));

        return new FinanceDashboardReportArpRespVO()
                .setPeriod(period)
                .setPreviousPeriod(previousPeriod)
                .setReceivableBalanceAmount(receivableBalanceAmount)
                .setPreviousReceivableBalanceAmount(previousReceivableBalanceAmount)
                .setReceivableBalanceChangeRate(calculateChangeRate(receivableBalanceAmount, previousReceivableBalanceAmount))
                .setPayableBalanceAmount(payableBalanceAmount)
                .setPreviousPayableBalanceAmount(previousPayableBalanceAmount)
                .setPayableBalanceChangeRate(calculateChangeRate(payableBalanceAmount, previousPayableBalanceAmount))
                .setAdvanceReceiptAmount(advanceReceiptAmount)
                .setPreviousAdvanceReceiptAmount(previousAdvanceReceiptAmount)
                .setAdvanceReceiptChangeRate(calculateChangeRate(advanceReceiptAmount, previousAdvanceReceiptAmount))
                .setReceivableContactLines(buildReportArpContactLines(
                        reqVO.getCompanyId(),
                        FinanceReceivablePayableTypeEnum.RECEIVABLE.name(),
                        currentRange))
                .setPayableContactLines(buildReportArpContactLines(
                        reqVO.getCompanyId(),
                        FinanceReceivablePayableTypeEnum.PAYABLE.name(),
                        currentRange));
    }

    @Override
    public FinanceDashboardAssetRespVO getAssetModule(FinanceDashboardMetricsReqVO reqVO) {
        validateCompany(reqVO.getCompanyId());
        String period = resolvePeriod(reqVO.getPeriod());
        String previousPeriod = resolvePreviousPeriod(period);
        FinanceDashboardMetricsRespVO currentMetrics = buildMetrics(reqVO.getCompanyId(), period);
        FinanceDashboardMetricsRespVO previousMetrics = buildMetrics(reqVO.getCompanyId(), previousPeriod);

        BigDecimal accountBalance = buildAccountBalanceByPeriod(reqVO.getCompanyId(), period);
        BigDecimal previousAccountBalance = buildAccountBalanceByPeriod(reqVO.getCompanyId(), previousPeriod);

        BigDecimal assetAmount = normalizeAmount(accountBalance.add(currentMetrics.getReceivableBalance()));
        BigDecimal liabilityAmount = normalizeAmount(currentMetrics.getPayableBalance());
        BigDecimal equityAmount = normalizeAmount(assetAmount.subtract(liabilityAmount));

        BigDecimal previousAssetAmount = normalizeAmount(previousAccountBalance.add(previousMetrics.getReceivableBalance()));
        BigDecimal previousLiabilityAmount = normalizeAmount(previousMetrics.getPayableBalance());
        BigDecimal previousEquityAmount = normalizeAmount(previousAssetAmount.subtract(previousLiabilityAmount));

        return new FinanceDashboardAssetRespVO()
                .setPeriod(period)
                .setPreviousPeriod(previousPeriod)
                .setAssetAmount(assetAmount)
                .setPreviousAssetAmount(previousAssetAmount)
                .setLiabilityAmount(liabilityAmount)
                .setPreviousLiabilityAmount(previousLiabilityAmount)
                .setEquityAmount(equityAmount)
                .setPreviousEquityAmount(previousEquityAmount);
    }

    @Override
    public FinanceDashboardCashflowRespVO getCashflowModule(FinanceDashboardMetricsReqVO reqVO) {
        validateCompany(reqVO.getCompanyId());
        String period = resolvePeriod(reqVO.getPeriod());
        String previousPeriod = resolvePreviousPeriod(period);
        FinanceDashboardMetricsRespVO currentMetrics = buildMetrics(reqVO.getCompanyId(), period);
        FinanceDashboardMetricsRespVO previousMetrics = buildMetrics(reqVO.getCompanyId(), previousPeriod);

        BigDecimal endingCashAmount = buildAccountBalanceByPeriod(reqVO.getCompanyId(), period);
        BigDecimal previousEndingCashAmount = buildAccountBalanceByPeriod(reqVO.getCompanyId(), previousPeriod);

        FinanceDashboardCashFlowActivitySummaryDTO activitySummary = financeTransactionMapper.selectDashboardCashFlowActivitySummary(
                reqVO.getCompanyId(), period, TRANSACTION_STATUS_SUCCESS, TRANSACTION_TYPE_INCOME, TRANSACTION_TYPE_EXPENSE);
        BigDecimal operatingCashFlow = normalizeAmount(activitySummary == null ? null : activitySummary.getOperatingNetCash());
        BigDecimal investingCashFlow = normalizeAmount(activitySummary == null ? null : activitySummary.getInvestingNetCash());
        BigDecimal financingCashFlow = normalizeAmount(activitySummary == null ? null : activitySummary.getFinancingNetCash());

        return new FinanceDashboardCashflowRespVO()
                .setPeriod(period)
                .setPreviousPeriod(previousPeriod)
                .setEndingCashAmount(endingCashAmount)
                .setPreviousEndingCashAmount(previousEndingCashAmount)
                .setNetCashIncreaseAmount(currentMetrics.getNetAmount())
                .setPreviousNetCashIncreaseAmount(previousMetrics.getNetAmount())
                .setOperatingCashFlowAmount(operatingCashFlow)
                .setInvestingCashFlowAmount(investingCashFlow)
                .setFinancingCashFlowAmount(financingCashFlow);
    }

    @Override
    public FinanceDashboardOverviewRespVO getOverview(FinanceDashboardMetricsReqVO reqVO) {
        validateCompany(reqVO.getCompanyId());
        String period = resolvePeriod(reqVO.getPeriod());
        FinanceDashboardMetricsRespVO metrics = buildMetrics(reqVO.getCompanyId(), period);
        FourWeekCashFlowResult fourWeekCashFlow = buildFourWeekCashFlowResult(reqVO.getCompanyId(), period);

        BigDecimal accountBalance = buildAccountBalanceByPeriod(reqVO.getCompanyId(), period);
        BigDecimal assetAmount = normalizeAmount(accountBalance.add(metrics.getReceivableBalance()));
        BigDecimal liabilityAmount = normalizeAmount(metrics.getPayableBalance());
        BigDecimal equityAmount = normalizeAmount(assetAmount.subtract(liabilityAmount));

        FinanceDashboardCashFlowActivitySummaryDTO activitySummary = financeTransactionMapper.selectDashboardCashFlowActivitySummary(
                reqVO.getCompanyId(), period, TRANSACTION_STATUS_SUCCESS, TRANSACTION_TYPE_INCOME, TRANSACTION_TYPE_EXPENSE);
        BigDecimal operatingCashFlow = normalizeAmount(activitySummary == null ? null : activitySummary.getOperatingNetCash());
        BigDecimal investingCashFlow = normalizeAmount(activitySummary == null ? null : activitySummary.getInvestingNetCash());
        BigDecimal financingCashFlow = normalizeAmount(activitySummary == null ? null : activitySummary.getFinancingNetCash());

        return new FinanceDashboardOverviewRespVO()
                .setPeriod(period)
                .setOperatingIncomeAmount(metrics.getIncomeAmount())
                .setOperatingExpenseAmount(metrics.getExpenseAmount())
                .setOperatingProfitAmount(metrics.getNetAmount())
                .setAccountBalanceAmount(accountBalance)
                .setReceivableBalanceAmount(metrics.getReceivableBalance())
                .setPayableBalanceAmount(metrics.getPayableBalance())
                .setWeeklyCashFlowList(fourWeekCashFlow.getWeeklyCashFlowList())
                .setAssetAmount(assetAmount)
                .setLiabilityAmount(liabilityAmount)
                .setEquityAmount(equityAmount)
                .setEndingCashAmount(accountBalance)
                .setNetCashIncreaseAmount(metrics.getNetAmount())
                .setOperatingCashFlowAmount(operatingCashFlow)
                .setInvestingCashFlowAmount(investingCashFlow)
                .setFinancingCashFlowAmount(financingCashFlow);
    }

    @Override
    public FinanceDashboardAggregateRespVO getAggregate(FinanceDashboardMetricsReqVO reqVO) {
        validateCompany(reqVO.getCompanyId());
        String period = resolvePeriod(reqVO.getPeriod());
        String previousPeriod = resolvePreviousPeriod(period);

        // 1. 共享基础数据 - 每个只查一次
        FinanceDashboardMetricsRespVO currentMetrics = buildMetrics(reqVO.getCompanyId(), period);
        FinanceDashboardMetricsRespVO previousMetrics = buildMetrics(reqVO.getCompanyId(), previousPeriod);
        BigDecimal currentAccountBalance = buildAccountBalanceByPeriod(reqVO.getCompanyId(), period);
        BigDecimal previousAccountBalance = buildAccountBalanceByPeriod(reqVO.getCompanyId(), previousPeriod);
        FourWeekCashFlowResult currentFourWeekCashFlow = buildFourWeekCashFlowResult(reqVO.getCompanyId(), period);
        FourWeekCashFlowResult previousFourWeekCashFlow = buildFourWeekCashFlowResult(reqVO.getCompanyId(), previousPeriod);
        FinanceDashboardCashFlowActivitySummaryDTO activitySummary = financeTransactionMapper.selectDashboardCashFlowActivitySummary(
                reqVO.getCompanyId(), period, TRANSACTION_STATUS_SUCCESS, TRANSACTION_TYPE_INCOME, TRANSACTION_TYPE_EXPENSE);

        // 2. 经营概览
        FinanceDashboardOperatingRespVO operating = new FinanceDashboardOperatingRespVO()
                .setPeriod(period)
                .setPreviousPeriod(previousPeriod)
                .setOperatingIncomeAmount(currentMetrics.getIncomeAmount())
                .setPreviousOperatingIncomeAmount(previousMetrics.getIncomeAmount())
                .setOperatingIncomeChangeRate(calculateChangeRate(
                        currentMetrics.getIncomeAmount(), previousMetrics.getIncomeAmount()))
                .setOperatingExpenseAmount(currentMetrics.getExpenseAmount())
                .setPreviousOperatingExpenseAmount(previousMetrics.getExpenseAmount())
                .setOperatingExpenseChangeRate(calculateChangeRate(
                        currentMetrics.getExpenseAmount(), previousMetrics.getExpenseAmount()))
                .setOperatingProfitAmount(currentMetrics.getNetAmount())
                .setPreviousOperatingProfitAmount(previousMetrics.getNetAmount())
                .setOperatingProfitChangeRate(calculateChangeRate(
                        currentMetrics.getNetAmount(), previousMetrics.getNetAmount()));

        // 3. 管理报表
        List<FinanceDashboardReportChannelBalanceRespVO> channelBalanceLines = buildReportChannelBalanceLines(
                reqVO.getCompanyId(), period, previousPeriod);
        FinanceDashboardReportRespVO report = new FinanceDashboardReportRespVO()
                .setPeriod(period)
                .setPreviousPeriod(previousPeriod)
                .setFourWeekCashFlowAmount(currentFourWeekCashFlow.getTotalNetCashFlowAmount())
                .setPreviousFourWeekCashFlowAmount(previousFourWeekCashFlow.getTotalNetCashFlowAmount())
                .setAccountBalanceAmount(currentAccountBalance)
                .setPreviousAccountBalanceAmount(previousAccountBalance)
                .setAccountBalanceChangeRate(calculateChangeRate(currentAccountBalance, previousAccountBalance))
                .setReceivableBalanceAmount(currentMetrics.getReceivableBalance())
                .setPreviousReceivableBalanceAmount(previousMetrics.getReceivableBalance())
                .setPayableBalanceAmount(currentMetrics.getPayableBalance())
                .setPreviousPayableBalanceAmount(previousMetrics.getPayableBalance())
                .setChannelBalanceLines(channelBalanceLines);

        // 4. 管理报表应收应付
        PeriodDateRange currentRange = buildPeriodDateRange(period);
        PeriodDateRange previousRange = buildPeriodDateRange(previousPeriod);
        FinanceDashboardArpBalanceSummaryDTO currentArpSummary = financeReceivablePayableMapper.selectDashboardArpBalanceSummaryByBillDateRange(
                reqVO.getCompanyId(),
                FinanceReceivablePayableTypeEnum.RECEIVABLE.name(),
                FinanceReceivablePayableTypeEnum.PAYABLE.name(),
                FinanceReceivablePayableStatusEnum.CANCELLED.name(),
                currentRange.getStartTime(), currentRange.getEndTime());
        FinanceDashboardArpBalanceSummaryDTO previousArpSummary = financeReceivablePayableMapper.selectDashboardArpBalanceSummaryByBillDateRange(
                reqVO.getCompanyId(),
                FinanceReceivablePayableTypeEnum.RECEIVABLE.name(),
                FinanceReceivablePayableTypeEnum.PAYABLE.name(),
                FinanceReceivablePayableStatusEnum.CANCELLED.name(),
                previousRange.getStartTime(), previousRange.getEndTime());
        BigDecimal receivableBalanceAmount = normalizeAmount(currentArpSummary == null ? null : currentArpSummary.getReceivableBalance());
        BigDecimal previousReceivableBalanceAmount = normalizeAmount(previousArpSummary == null ? null : previousArpSummary.getReceivableBalance());
        BigDecimal payableBalanceAmount = normalizeAmount(currentArpSummary == null ? null : currentArpSummary.getPayableBalance());
        BigDecimal previousPayableBalanceAmount = normalizeAmount(previousArpSummary == null ? null : previousArpSummary.getPayableBalance());
        BigDecimal advanceReceiptAmount = normalizeAmount(
                financeReceivablePayableMapper.selectDashboardAdvanceReceiptByBillDateRange(
                        reqVO.getCompanyId(),
                        FinanceReceivablePayableTypeEnum.ADVANCE_RECEIPT.name(),
                        FinanceReceivablePayableStatusEnum.CANCELLED.name(),
                        currentRange.getStartTime(), currentRange.getEndTime()));
        BigDecimal previousAdvanceReceiptAmount = normalizeAmount(
                financeReceivablePayableMapper.selectDashboardAdvanceReceiptByBillDateRange(
                        reqVO.getCompanyId(),
                        FinanceReceivablePayableTypeEnum.ADVANCE_RECEIPT.name(),
                        FinanceReceivablePayableStatusEnum.CANCELLED.name(),
                        previousRange.getStartTime(), previousRange.getEndTime()));
        FinanceDashboardReportArpRespVO reportArp = new FinanceDashboardReportArpRespVO()
                .setPeriod(period)
                .setPreviousPeriod(previousPeriod)
                .setReceivableBalanceAmount(receivableBalanceAmount)
                .setPreviousReceivableBalanceAmount(previousReceivableBalanceAmount)
                .setReceivableBalanceChangeRate(calculateChangeRate(receivableBalanceAmount, previousReceivableBalanceAmount))
                .setPayableBalanceAmount(payableBalanceAmount)
                .setPreviousPayableBalanceAmount(previousPayableBalanceAmount)
                .setPayableBalanceChangeRate(calculateChangeRate(payableBalanceAmount, previousPayableBalanceAmount))
                .setAdvanceReceiptAmount(advanceReceiptAmount)
                .setPreviousAdvanceReceiptAmount(previousAdvanceReceiptAmount)
                .setAdvanceReceiptChangeRate(calculateChangeRate(advanceReceiptAmount, previousAdvanceReceiptAmount))
                .setReceivableContactLines(buildReportArpContactLines(
                        reqVO.getCompanyId(), FinanceReceivablePayableTypeEnum.RECEIVABLE.name(), currentRange))
                .setPayableContactLines(buildReportArpContactLines(
                        reqVO.getCompanyId(), FinanceReceivablePayableTypeEnum.PAYABLE.name(), currentRange));

        // 5. 资产负债
        BigDecimal assetAmount = normalizeAmount(currentAccountBalance.add(currentMetrics.getReceivableBalance()));
        BigDecimal liabilityAmount = normalizeAmount(currentMetrics.getPayableBalance());
        BigDecimal equityAmount = normalizeAmount(assetAmount.subtract(liabilityAmount));
        BigDecimal previousAssetAmount = normalizeAmount(previousAccountBalance.add(previousMetrics.getReceivableBalance()));
        BigDecimal previousLiabilityAmount = normalizeAmount(previousMetrics.getPayableBalance());
        BigDecimal previousEquityAmount = normalizeAmount(previousAssetAmount.subtract(previousLiabilityAmount));
        FinanceDashboardAssetRespVO asset = new FinanceDashboardAssetRespVO()
                .setPeriod(period)
                .setPreviousPeriod(previousPeriod)
                .setAssetAmount(assetAmount)
                .setPreviousAssetAmount(previousAssetAmount)
                .setLiabilityAmount(liabilityAmount)
                .setPreviousLiabilityAmount(previousLiabilityAmount)
                .setEquityAmount(equityAmount)
                .setPreviousEquityAmount(previousEquityAmount);

        // 6. 现金流量统计
        BigDecimal operatingCashFlow = normalizeAmount(activitySummary == null ? null : activitySummary.getOperatingNetCash());
        BigDecimal investingCashFlow = normalizeAmount(activitySummary == null ? null : activitySummary.getInvestingNetCash());
        BigDecimal financingCashFlow = normalizeAmount(activitySummary == null ? null : activitySummary.getFinancingNetCash());
        FinanceDashboardCashflowRespVO cashflow = new FinanceDashboardCashflowRespVO()
                .setPeriod(period)
                .setPreviousPeriod(previousPeriod)
                .setEndingCashAmount(currentAccountBalance)
                .setPreviousEndingCashAmount(previousAccountBalance)
                .setNetCashIncreaseAmount(currentMetrics.getNetAmount())
                .setPreviousNetCashIncreaseAmount(previousMetrics.getNetAmount())
                .setOperatingCashFlowAmount(operatingCashFlow)
                .setInvestingCashFlowAmount(investingCashFlow)
                .setFinancingCashFlowAmount(financingCashFlow);

        return new FinanceDashboardAggregateRespVO()
                .setOperating(operating)
                .setReport(report)
                .setReportArp(reportArp)
                .setAsset(asset)
                .setCashflow(cashflow);
    }


    private BigDecimal buildAccountBalanceByPeriod(Long companyId, String period) {
        return normalizeAmount(financeAccountMapper.selectDashboardAccountBalanceByCompanyUsageScopeAndPeriod(
                companyId, period, TRANSACTION_STATUS_SUCCESS, TRANSACTION_TYPE_INCOME, TRANSACTION_TYPE_EXPENSE));
    }

    private FinanceDashboardMetricsRespVO buildMetrics(Long companyId, String period) {
        FinanceDashboardTransactionSummaryDTO transactionSummary = financeTransactionMapper.selectDashboardTransactionSummary(
                companyId,
                period,
                TRANSACTION_STATUS_SUCCESS,
                TRANSACTION_TYPE_INCOME,
                TRANSACTION_TYPE_EXPENSE);
        PeriodDateRange periodDateRange = buildPeriodDateRange(period);
        FinanceDashboardArpBalanceSummaryDTO arpSummary = financeReceivablePayableMapper.selectDashboardArpBalanceSummaryByBillDateRange(
                companyId,
                FinanceReceivablePayableTypeEnum.RECEIVABLE.name(),
                FinanceReceivablePayableTypeEnum.PAYABLE.name(),
                FinanceReceivablePayableStatusEnum.CANCELLED.name(),
                periodDateRange.getStartTime(),
                periodDateRange.getEndTime());

        BigDecimal incomeAmount = normalizeAmount(transactionSummary == null ? null : transactionSummary.getIncomeAmount());
        BigDecimal expenseAmount = normalizeAmount(transactionSummary == null ? null : transactionSummary.getExpenseAmount());
        BigDecimal netAmount = normalizeAmount(incomeAmount.subtract(expenseAmount));
        BigDecimal receivableBalance = normalizeAmount(arpSummary == null ? null : arpSummary.getReceivableBalance());
        BigDecimal payableBalance = normalizeAmount(arpSummary == null ? null : arpSummary.getPayableBalance());

        return new FinanceDashboardMetricsRespVO()
                .setPeriod(period)
                .setIncomeAmount(incomeAmount)
                .setExpenseAmount(expenseAmount)
                .setNetAmount(netAmount)
                .setReceivableBalance(receivableBalance)
                .setPayableBalance(payableBalance);
    }

    private FourWeekCashFlowResult buildFourWeekCashFlowResult(Long companyId, String period) {
        FourWeekDateRange range = buildFourWeekDateRange(period);

        List<FinanceDashboardWeeklyCashFlowDTO> rawList = financeTransactionMapper.selectDashboardWeeklyCashFlow(
                companyId,
                range.getStartTime(),
                range.getEndTime(),
                TRANSACTION_STATUS_SUCCESS,
                TRANSACTION_TYPE_INCOME,
                TRANSACTION_TYPE_EXPENSE);
        Map<String, FinanceDashboardWeeklyCashFlowDTO> weekSummaryMap = new HashMap<>();
        if (rawList != null) {
            for (FinanceDashboardWeeklyCashFlowDTO item : rawList) {
                if (item != null && StringUtils.hasText(item.getWeekStartDate())) {
                    weekSummaryMap.put(StringUtils.trimWhitespace(item.getWeekStartDate()), item);
                }
            }
        }

        List<FinanceDashboardWeeklyCashFlowRespVO> weeklyCashFlowList = new ArrayList<>(FOUR_WEEK_COUNT);
        BigDecimal totalNetCashFlowAmount = BigDecimal.ZERO;
        for (int i = 0; i < FOUR_WEEK_COUNT; i++) {
            LocalDate weekStart = range.getFirstWeekStartDate().plusWeeks(i);
            String weekKey = weekStart.format(WEEK_DATE_FORMATTER);
            FinanceDashboardWeeklyCashFlowDTO summary = weekSummaryMap.get(weekKey);
            BigDecimal incomeAmount = normalizeAmount(summary == null ? null : summary.getIncomeAmount());
            BigDecimal expenseAmount = normalizeAmount(summary == null ? null : summary.getExpenseAmount());
            BigDecimal netCashFlowAmount = normalizeAmount(incomeAmount.subtract(expenseAmount));
            totalNetCashFlowAmount = totalNetCashFlowAmount.add(netCashFlowAmount);
            weeklyCashFlowList.add(new FinanceDashboardWeeklyCashFlowRespVO()
                    .setWeekStartDate(weekKey)
                    .setIncomeAmount(incomeAmount)
                    .setExpenseAmount(expenseAmount)
                    .setNetCashFlowAmount(netCashFlowAmount));
        }
        return new FourWeekCashFlowResult(weeklyCashFlowList, normalizeAmount(totalNetCashFlowAmount));
    }

    private FourWeekDateRange buildFourWeekDateRange(String period) {
        YearMonth yearMonth = YearMonth.parse(period, PERIOD_FORMATTER);
        YearMonth currentYearMonth = YearMonth.now();
        LocalDate anchorDate = yearMonth.equals(currentYearMonth)
                ? LocalDate.now()
                : yearMonth.atEndOfMonth();
        LocalDate anchorWeekStart = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate firstWeekStart = anchorWeekStart.minusWeeks(FOUR_WEEK_COUNT - 1L);
        return new FourWeekDateRange(
                firstWeekStart,
                firstWeekStart.atStartOfDay(),
                anchorWeekStart.plusWeeks(1).atStartOfDay());
    }

    private void validateCompany(Long companyId) {
        FinanceCompanyDO companyDO = companyId == null ? null : financeCompanyMapper.selectById(companyId);
        if (companyDO == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
    }

    private String resolvePeriod(String period) {
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

    private List<FinanceDashboardReportArpContactLineRespVO> buildReportArpContactLines(Long companyId,
                                                                                         String type,
                                                                                         PeriodDateRange range) {
        List<FinanceDashboardArpContactSummaryDTO> summaryList = financeReceivablePayableMapper.selectDashboardArpContactSummaryByBillDateRange(
                companyId,
                type,
                FinanceReceivablePayableStatusEnum.CANCELLED.name(),
                range.getStartTime(),
                range.getEndTime(),
                REPORT_ARP_CONTACT_LIMIT);
        if (summaryList == null || summaryList.isEmpty()) {
            return Collections.emptyList();
        }
        return summaryList.stream()
                .map(item -> new FinanceDashboardReportArpContactLineRespVO()
                        .setContactId(item.getContactId())
                        .setContactName(StringUtils.hasText(item.getContactName())
                                ? StringUtils.trimWhitespace(item.getContactName())
                                : "ID:" + item.getContactId())
                        .setAmount(normalizeAmount(item.getAmount())))
                .collect(Collectors.toList());
    }

    private List<FinanceDashboardReportChannelBalanceRespVO> buildReportChannelBalanceLines(Long companyId,
                                                                                             String period,
                                                                                             String previousPeriod) {
        Map<String, BigDecimal> currentMap = buildChannelBalanceMap(companyId, period);
        Map<String, BigDecimal> previousMap = buildChannelBalanceMap(companyId, previousPeriod);
        List<String> channelTypes = new ArrayList<>(3);
        channelTypes.add(ACCOUNT_TYPE_WECHAT);
        channelTypes.add(ACCOUNT_TYPE_ALIPAY);
        channelTypes.add(ACCOUNT_TYPE_BANK);
        List<FinanceDashboardReportChannelBalanceRespVO> result = new ArrayList<>(channelTypes.size());
        for (String channelType : channelTypes) {
            BigDecimal currentAmount = normalizeAmount(currentMap.get(channelType));
            BigDecimal previousAmount = normalizeAmount(previousMap.get(channelType));
            result.add(new FinanceDashboardReportChannelBalanceRespVO()
                    .setChannelType(channelType)
                    .setChannelName(resolveChannelName(channelType))
                    .setAmount(currentAmount)
                    .setPreviousAmount(previousAmount)
                    .setChangeRate(calculateChangeRate(currentAmount, previousAmount)));
        }
        return result;
    }

    private Map<String, BigDecimal> buildChannelBalanceMap(Long companyId, String period) {
        List<FinanceDashboardChannelBalanceSummaryDTO> summaryList = financeAccountMapper
                .selectDashboardChannelBalanceByCompanyUsageScopeAndPeriod(
                        companyId,
                        period,
                        TRANSACTION_STATUS_SUCCESS,
                        TRANSACTION_TYPE_INCOME,
                        TRANSACTION_TYPE_EXPENSE,
                        TRANSACTION_TYPE_TRANSFER,
                        ACCOUNT_TYPE_BANK,
                        ACCOUNT_TYPE_ALIPAY,
                        ACCOUNT_TYPE_WECHAT);
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        map.put(ACCOUNT_TYPE_WECHAT, BigDecimal.ZERO);
        map.put(ACCOUNT_TYPE_ALIPAY, BigDecimal.ZERO);
        map.put(ACCOUNT_TYPE_BANK, BigDecimal.ZERO);
        if (summaryList == null || summaryList.isEmpty()) {
            return map;
        }
        for (FinanceDashboardChannelBalanceSummaryDTO item : summaryList) {
            if (item == null || !StringUtils.hasText(item.getAccountType())) {
                continue;
            }
            map.put(StringUtils.trimWhitespace(item.getAccountType()).toUpperCase(), normalizeAmount(item.getBalance()));
        }
        return map;
    }

    private String resolveChannelName(String channelType) {
        if (ACCOUNT_TYPE_WECHAT.equals(channelType)) {
            return "微信";
        }
        if (ACCOUNT_TYPE_ALIPAY.equals(channelType)) {
            return "支付宝";
        }
        return "银行";
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

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static class PeriodDateRange {

        private final LocalDateTime startTime;

        private final LocalDateTime endTime;

        private PeriodDateRange(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }
    }

    private static class FourWeekDateRange {

        private final LocalDate firstWeekStartDate;

        private final LocalDateTime startTime;

        private final LocalDateTime endTime;

        private FourWeekDateRange(LocalDate firstWeekStartDate, LocalDateTime startTime, LocalDateTime endTime) {
            this.firstWeekStartDate = firstWeekStartDate;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalDate getFirstWeekStartDate() {
            return firstWeekStartDate;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }
    }

    private static class FourWeekCashFlowResult {

        private final List<FinanceDashboardWeeklyCashFlowRespVO> weeklyCashFlowList;

        private final BigDecimal totalNetCashFlowAmount;

        private FourWeekCashFlowResult(List<FinanceDashboardWeeklyCashFlowRespVO> weeklyCashFlowList,
                                       BigDecimal totalNetCashFlowAmount) {
            this.weeklyCashFlowList = weeklyCashFlowList;
            this.totalNetCashFlowAmount = totalNetCashFlowAmount;
        }

        public List<FinanceDashboardWeeklyCashFlowRespVO> getWeeklyCashFlowList() {
            return weeklyCashFlowList;
        }

        public BigDecimal getTotalNetCashFlowAmount() {
            return totalNetCashFlowAmount;
        }
    }
}
