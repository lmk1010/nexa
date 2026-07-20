package com.kyx.service.finance.service.report.impl;

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
import com.kyx.service.finance.service.report.dto.FinanceIncomeStatementAggregateDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceReportServiceImplTest {

    @Mock
    private FinanceTransactionMapper financeTransactionMapper;
    @Mock
    private FinanceReceivablePayableMapper financeReceivablePayableMapper;
    @Mock
    private FinanceAccountMapper financeAccountMapper;
    @Mock
    private FinanceCompanyMapper financeCompanyMapper;
    @Mock
    private FinanceCompanySubjectMapper financeCompanySubjectMapper;
    @Mock
    private FinanceReportSnapshotMapper financeReportSnapshotMapper;

    @InjectMocks
    private FinanceReportServiceImpl financeReportService;

    @Test
    void getIncomeStatement_shouldUseSnapshotDataWhenSinglePeriod() {
        Long companyId = 1L;
        FinanceIncomeStatementReqVO reqVO = new FinanceIncomeStatementReqVO();
        reqVO.setCompanyId(companyId);
        reqVO.setPeriod("202603");

        when(financeCompanyMapper.selectById(companyId)).thenReturn(new FinanceCompanyDO().setId(companyId));
        when(financeCompanySubjectMapper.selectListByCompanyId(companyId))
                .thenReturn(Collections.singletonList(
                        new FinanceCompanySubjectDO()
                                .setId(11L)
                                .setSubjectCode("6001")
                                .setSubjectName("主营业务收入")
                                .setSubjectType("INCOME")
                                .setParentCode("0")
                ));
        when(financeReportSnapshotMapper.selectByCompanyIdAndReportCodeAndPeriod(companyId, "INCOME_STATEMENT", "202603"))
                .thenReturn(new FinanceReportSnapshotDO().setDataJson(
                        "{\"aggregateList\":[{\"subjectCode\":\"6001\",\"incomeAmount\":1000.00,\"expenseAmount\":0}]}"
                ));

        List<FinanceIncomeStatementRespVO> result = financeReportService.getIncomeStatement(reqVO);

        assertNotNull(result);
        FinanceIncomeStatementRespVO detail = result.stream()
                .filter(item -> "6001".equals(item.getItemCode()))
                .findFirst()
                .orElse(null);
        FinanceIncomeStatementRespVO total = result.stream()
                .filter(item -> "TOTAL".equals(item.getItemCode()))
                .findFirst()
                .orElse(null);
        assertNotNull(detail);
        assertNotNull(total);
        assertEquals(new BigDecimal("1000.00"), detail.getCurrentIncome());
        assertEquals(new BigDecimal("1000.00"), total.getCurrentIncome());
        verify(financeTransactionMapper, never())
                .selectIncomeStatementAggregateByPeriodRange(eq(companyId), anyString(), anyString(),
                        anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void getIncomeStatement_shouldFallbackToRealtimeWhenSnapshotInvalid() {
        Long companyId = 2L;
        FinanceIncomeStatementReqVO reqVO = new FinanceIncomeStatementReqVO();
        reqVO.setCompanyId(companyId);
        reqVO.setPeriod("202603");

        when(financeCompanyMapper.selectById(companyId)).thenReturn(new FinanceCompanyDO().setId(companyId));
        when(financeCompanySubjectMapper.selectListByCompanyId(companyId))
                .thenReturn(Collections.singletonList(
                        new FinanceCompanySubjectDO()
                                .setId(12L)
                                .setSubjectCode("6001")
                                .setSubjectName("主营业务收入")
                                .setSubjectType("INCOME")
                                .setParentCode("0")
                ));
        when(financeReportSnapshotMapper.selectByCompanyIdAndReportCodeAndPeriod(companyId, "INCOME_STATEMENT", "202603"))
                .thenReturn(new FinanceReportSnapshotDO().setDataJson("{invalid-json"));

        FinanceIncomeStatementAggregateDTO aggregateDTO = new FinanceIncomeStatementAggregateDTO();
        aggregateDTO.setSubjectCode("6001");
        aggregateDTO.setIncomeAmount(new BigDecimal("500"));
        aggregateDTO.setExpenseAmount(BigDecimal.ZERO);
        when(financeTransactionMapper.selectIncomeStatementAggregateByPeriodRange(
                companyId, "202603", "202603", "SUCCESS", "INCOME", "EXPENSE",
                "__UNCLASSIFIED_INCOME__", "__UNCLASSIFIED_EXPENSE__"))
                .thenReturn(Collections.singletonList(aggregateDTO));

        List<FinanceIncomeStatementRespVO> result = financeReportService.getIncomeStatement(reqVO);

        FinanceIncomeStatementRespVO total = result.stream()
                .filter(item -> "TOTAL".equals(item.getItemCode()))
                .findFirst()
                .orElse(null);
        assertNotNull(total);
        assertEquals(new BigDecimal("500.00"), total.getCurrentIncome());
        verify(financeTransactionMapper)
                .selectIncomeStatementAggregateByPeriodRange(
                        companyId, "202603", "202603", "SUCCESS", "INCOME", "EXPENSE",
                        "__UNCLASSIFIED_INCOME__", "__UNCLASSIFIED_EXPENSE__");
    }
}
