package com.kyx.service.finance.dal.mysql.transaction;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionPageReqVO;
import com.kyx.service.finance.dal.dataobject.transaction.FinanceTransactionDO;
import com.kyx.service.finance.enums.FinanceTransactionStatusEnum;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardCashFlowActivitySummaryDTO;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardTransactionSummaryDTO;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardWeeklyCashFlowDTO;
import com.kyx.service.finance.service.report.dto.FinanceIncomeStatementAggregateDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资金流水 Mapper
 * @author xyang
 */
@Mapper
public interface FinanceTransactionMapper extends BaseMapperX<FinanceTransactionDO> {

    List<FinanceIncomeStatementAggregateDTO> selectIncomeStatementAggregateByPeriodRange(
            @Param("companyId") Long companyId,
            @Param("startPeriod") String startPeriod,
            @Param("endPeriod") String endPeriod,
            @Param("successStatus") String successStatus,
            @Param("incomeType") String incomeType,
            @Param("expenseType") String expenseType,
            @Param("unclassifiedIncomeCode") String unclassifiedIncomeCode,
            @Param("unclassifiedExpenseCode") String unclassifiedExpenseCode);

    FinanceDashboardTransactionSummaryDTO selectDashboardTransactionSummary(
            @Param("companyId") Long companyId,
            @Param("period") String period,
            @Param("successStatus") String successStatus,
            @Param("incomeType") String incomeType,
            @Param("expenseType") String expenseType);

    List<FinanceDashboardWeeklyCashFlowDTO> selectDashboardWeeklyCashFlow(
            @Param("companyId") Long companyId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("successStatus") String successStatus,
            @Param("incomeType") String incomeType,
            @Param("expenseType") String expenseType);

    FinanceDashboardCashFlowActivitySummaryDTO selectDashboardCashFlowActivitySummary(
            @Param("companyId") Long companyId,
            @Param("period") String period,
            @Param("successStatus") String successStatus,
            @Param("incomeType") String incomeType,
            @Param("expenseType") String expenseType);

    default boolean existsByTransactionNo(Long companyId, String transactionNo, Long excludeId) {
        if (!StringUtils.hasText(transactionNo) || companyId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceTransactionDO>()
                .eq(FinanceTransactionDO::getCompanyId, companyId)
                .eq(FinanceTransactionDO::getTransactionNo, transactionNo)
                .neIfPresent(FinanceTransactionDO::getId, excludeId)) > 0;
    }

    default boolean existsDraftByCompanyIdAndPeriod(Long companyId, String transactionPeriod) {
        return countDraftByCompanyIdAndPeriod(companyId, transactionPeriod) > 0;
    }

    default boolean existsByAccountId(Long accountId) {
        if (accountId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceTransactionDO>()
                .and(wrapper -> wrapper.eq(FinanceTransactionDO::getAccountId, accountId)
                        .or()
                        .eq(FinanceTransactionDO::getOppositeAccountId, accountId))) > 0;
    }

    default boolean existsByContactId(Long contactId) {
        if (contactId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceTransactionDO>()
                .eq(FinanceTransactionDO::getContactId, contactId)) > 0;
    }

    default int countDraftByCompanyIdAndPeriod(Long companyId, String transactionPeriod) {
        if (companyId == null || !StringUtils.hasText(transactionPeriod)) {
            return 0;
        }
        return Math.toIntExact(selectCount(new LambdaQueryWrapperX<FinanceTransactionDO>()
                .eq(FinanceTransactionDO::getCompanyId, companyId)
                .eq(FinanceTransactionDO::getTransactionPeriod, StringUtils.trimWhitespace(transactionPeriod))
                .eq(FinanceTransactionDO::getStatus, FinanceTransactionStatusEnum.DRAFT.name())));
    }

    default PageResult<FinanceTransactionDO> selectPage(FinanceTransactionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FinanceTransactionDO>()
                .eqIfPresent(FinanceTransactionDO::getCompanyId, reqVO.getCompanyId())
                .likeIfPresent(FinanceTransactionDO::getTransactionNo, StringUtils.trimWhitespace(reqVO.getTransactionNo()))
                .eqIfPresent(FinanceTransactionDO::getTransactionType, reqVO.getTransactionType())
                .eqIfPresent(FinanceTransactionDO::getStatus, reqVO.getStatus())
                .eqIfPresent(FinanceTransactionDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(FinanceTransactionDO::getOppositeAccountId, reqVO.getOppositeAccountId())
                .likeIfPresent(FinanceTransactionDO::getSubjectCode, StringUtils.trimWhitespace(reqVO.getSubjectCode()))
                .eqIfPresent(FinanceTransactionDO::getContactId, reqVO.getContactId())
                .likeIfPresent(FinanceTransactionDO::getCategory, StringUtils.trimWhitespace(reqVO.getCategory()))
                .likeIfPresent(FinanceTransactionDO::getBusinessRefNo, StringUtils.trimWhitespace(reqVO.getBusinessRefNo()))
                .eqIfPresent(FinanceTransactionDO::getBusinessType, reqVO.getBusinessType())
                .betweenIfPresent(FinanceTransactionDO::getTransactionDate, reqVO.getTransactionDate())
                .orderByDesc(FinanceTransactionDO::getId)
                .orderByDesc(BaseDO::getCreateTime));
    }
}
