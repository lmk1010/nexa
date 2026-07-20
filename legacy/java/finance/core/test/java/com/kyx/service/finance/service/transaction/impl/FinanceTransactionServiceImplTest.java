package com.kyx.service.finance.service.transaction.impl;

import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionSaveReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceAccountDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanyDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanySubjectDO;
import com.kyx.service.finance.dal.dataobject.transaction.FinanceTransactionDO;
import com.kyx.service.finance.dal.mysql.init.FinanceAccountMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanyMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanySubjectMapper;
import com.kyx.service.finance.dal.mysql.transaction.FinanceTransactionMapper;
import com.kyx.service.finance.enums.FinanceTransactionStatusEnum;
import com.kyx.service.finance.enums.FinanceTransactionTypeEnum;
import com.kyx.service.finance.service.support.FinancePeriodGuardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceTransactionServiceImplTest {

    @Mock
    private FinanceTransactionMapper financeTransactionMapper;
    @Mock
    private FinanceAccountMapper financeAccountMapper;
    @Mock
    private FinanceCompanyMapper financeCompanyMapper;
    @Mock
    private FinanceCompanySubjectMapper financeCompanySubjectMapper;
    @Mock
    private FinancePeriodGuardService financePeriodGuardService;

    @InjectMocks
    private FinanceTransactionServiceImpl financeTransactionService;

    @Test
    void createTransaction_allocationSuccess_shouldNotChangeAccountBalance() {
        Long companyId = 10L;
        Long accountId = 20L;
        String transactionNo = "TRX-ALLOC-001";
        String subjectCode = "6601";

        FinanceTransactionSaveReqVO reqVO = new FinanceTransactionSaveReqVO();
        reqVO.setCompanyId(companyId);
        reqVO.setAccountId(accountId);
        reqVO.setTransactionNo(transactionNo);
        reqVO.setTransactionDate(LocalDateTime.of(2026, 3, 5, 10, 0, 0));
        reqVO.setAmount(new BigDecimal("88.66"));
        reqVO.setTransactionType(FinanceTransactionTypeEnum.ALLOCATION.name());
        reqVO.setSubjectCode(subjectCode);
        reqVO.setStatus(FinanceTransactionStatusEnum.SUCCESS.name());
        reqVO.setCategory(null);

        when(financeAccountMapper.selectById(accountId)).thenReturn(new FinanceAccountDO().setId(accountId));
        when(financeCompanyMapper.selectById(companyId)).thenReturn(new FinanceCompanyDO().setId(companyId));
        when(financeTransactionMapper.existsByTransactionNo(companyId, transactionNo, null)).thenReturn(false);
        when(financeCompanySubjectMapper.selectEnabledByCompanyIdAndSubjectCode(companyId, subjectCode))
                .thenReturn(new FinanceCompanySubjectDO().setSubjectType("EXPENSE"));
        doAnswer(invocation -> {
            FinanceTransactionDO transactionDO = invocation.getArgument(0);
            transactionDO.setId(999L);
            return 1;
        }).when(financeTransactionMapper).insert(any(FinanceTransactionDO.class));

        Long transactionId = financeTransactionService.createTransaction(reqVO);

        assertEquals(999L, transactionId);
        ArgumentCaptor<FinanceTransactionDO> captor = ArgumentCaptor.forClass(FinanceTransactionDO.class);
        verify(financeTransactionMapper).insert(captor.capture());
        FinanceTransactionDO savedDO = captor.getValue();
        assertEquals("INTERNAL", savedDO.getBusinessType());
        assertEquals("EXPENSE_ALLOCATION", savedDO.getCategory());
        assertEquals(FinanceTransactionTypeEnum.ALLOCATION.name(), savedDO.getTransactionType());
        assertNull(savedDO.getOppositeAccountId());

        verify(financeAccountMapper, never()).increaseBalance(anyLong(), any(BigDecimal.class));
        verify(financeAccountMapper, never()).decreaseBalance(anyLong(), any(BigDecimal.class));
        verify(financePeriodGuardService).validateDateEditable(eq(companyId), eq(reqVO.getTransactionDate()));
    }
}
