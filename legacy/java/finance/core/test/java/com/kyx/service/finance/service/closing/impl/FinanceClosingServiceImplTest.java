package com.kyx.service.finance.service.closing.impl;

import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.service.finance.dal.dataobject.report.FinanceReportSnapshotDO;
import com.kyx.service.finance.dal.mysql.closing.FinanceClosingMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanyMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanySubjectMapper;
import com.kyx.service.finance.dal.mysql.report.FinanceReportSnapshotMapper;
import com.kyx.service.finance.dal.mysql.transaction.FinanceTransactionMapper;
import com.kyx.service.finance.dal.mysql.voucher.FinanceVoucherDetailMapper;
import com.kyx.service.finance.dal.mysql.voucher.FinanceVoucherMapper;
import com.kyx.service.finance.service.report.dto.FinanceIncomeStatementAggregateDTO;
import com.kyx.service.finance.service.support.FinancePeriodGuardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceClosingServiceImplTest {

    @Mock
    private FinanceClosingMapper financeClosingMapper;
    @Mock
    private FinanceCompanyMapper financeCompanyMapper;
    @Mock
    private FinanceCompanySubjectMapper financeCompanySubjectMapper;
    @Mock
    private FinanceTransactionMapper financeTransactionMapper;
    @Mock
    private FinanceVoucherMapper financeVoucherMapper;
    @Mock
    private FinanceVoucherDetailMapper financeVoucherDetailMapper;
    @Mock
    private FinanceReportSnapshotMapper financeReportSnapshotMapper;
    @Mock
    private FinancePeriodGuardService financePeriodGuardService;

    @InjectMocks
    private FinanceClosingServiceImpl financeClosingService;

    @Test
    void saveIncomeStatementSnapshot_shouldInsertSnapshotWhenNotExists() {
        Long companyId = 66L;
        String period = "202603";

        FinanceIncomeStatementAggregateDTO aggregateDTO = new FinanceIncomeStatementAggregateDTO();
        aggregateDTO.setSubjectCode("6001");
        aggregateDTO.setIncomeAmount(new BigDecimal("321.00"));
        aggregateDTO.setExpenseAmount(BigDecimal.ZERO);
        when(financeTransactionMapper.selectIncomeStatementAggregateByPeriodRange(
                companyId, period, period, "SUCCESS", "INCOME", "EXPENSE",
                "__UNCLASSIFIED_INCOME__", "__UNCLASSIFIED_EXPENSE__"))
                .thenReturn(Collections.singletonList(aggregateDTO));
        when(financeReportSnapshotMapper.selectByCompanyIdAndReportCodeAndPeriod(companyId, "INCOME_STATEMENT", period))
                .thenReturn(null);

        ReflectionTestUtils.invokeMethod(financeClosingService, "saveIncomeStatementSnapshot", companyId, period);

        ArgumentCaptor<FinanceReportSnapshotDO> captor = ArgumentCaptor.forClass(FinanceReportSnapshotDO.class);
        verify(financeReportSnapshotMapper).insert(captor.capture());
        FinanceReportSnapshotDO saved = captor.getValue();
        assertEquals(companyId, saved.getCompanyId());
        assertEquals("INCOME_STATEMENT", saved.getReportCode());
        assertEquals(period, saved.getSnapshotPeriod());
        assertNotNull(saved.getSnapshotDate());
        Map<String, Object> payload = JsonUtils.parseObject(saved.getDataJson(), Map.class);
        assertNotNull(payload);
    }

    @Test
    void deleteIncomeStatementSnapshot_shouldDelegateMapperDelete() {
        Long companyId = 77L;
        String period = "202602";

        ReflectionTestUtils.invokeMethod(financeClosingService, "deleteIncomeStatementSnapshot", companyId, period);

        verify(financeReportSnapshotMapper).deleteByCompanyIdAndReportCodeAndPeriod(
                companyId, "INCOME_STATEMENT", period);
    }
}
