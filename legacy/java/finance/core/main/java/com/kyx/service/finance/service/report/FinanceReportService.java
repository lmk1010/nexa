package com.kyx.service.finance.service.report;

import com.kyx.service.finance.controller.admin.report.vo.FinanceIncomeStatementReqVO;
import com.kyx.service.finance.controller.admin.report.vo.FinanceAssetStatementRespVO;
import com.kyx.service.finance.controller.admin.report.vo.FinanceCashflowStatementRespVO;
import com.kyx.service.finance.controller.admin.report.vo.FinanceIncomeStatementRespVO;

import java.util.List;

/**
 * 财务报表 Service
 */
public interface FinanceReportService {

    /**
     * 利润表查询
     */
    List<FinanceIncomeStatementRespVO> getIncomeStatement(FinanceIncomeStatementReqVO reqVO);

    /**
     * 资产负债表查询（按期间月）
     */
    FinanceAssetStatementRespVO getAssetStatement(FinanceIncomeStatementReqVO reqVO);

    /**
     * 现金流量表查询（按期间月）
     */
    FinanceCashflowStatementRespVO getCashflowStatement(FinanceIncomeStatementReqVO reqVO);
}
