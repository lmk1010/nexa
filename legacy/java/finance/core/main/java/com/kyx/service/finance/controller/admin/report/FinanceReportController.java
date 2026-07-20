package com.kyx.service.finance.controller.admin.report;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.finance.controller.admin.report.vo.FinanceAssetStatementRespVO;
import com.kyx.service.finance.controller.admin.report.vo.FinanceCashflowStatementRespVO;
import com.kyx.service.finance.controller.admin.report.vo.FinanceIncomeStatementReqVO;
import com.kyx.service.finance.controller.admin.report.vo.FinanceIncomeStatementRespVO;
import com.kyx.service.finance.service.report.FinanceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 财务报表 Controller
 */
@RestController
@RequestMapping("/finance/report")
@Tag(name = "财务管理 - 报表")
@Validated
public class FinanceReportController {

    @Resource
    private FinanceReportService financeReportService;

    @Operation(summary = "利润表查询（按期间月查；期间为空按当前年份统计）")
    @GetMapping("/income-statement/list")
    @PreAuthorize("@ss.hasPermission('finance:report:income-statement:list')")
    public CommonResult<List<FinanceIncomeStatementRespVO>> getIncomeStatement(
            @Valid FinanceIncomeStatementReqVO reqVO) {
        return success(financeReportService.getIncomeStatement(reqVO));
    }

    @Operation(summary = "资产负债表查询（按期间月查；期间为空按当前月份）")
    @GetMapping("/asset-statement/get")
    @PreAuthorize("@ss.hasAnyPermissions('finance:report:asset-statement:list,finance:report:income-statement:list')")
    public CommonResult<FinanceAssetStatementRespVO> getAssetStatement(
            @Valid FinanceIncomeStatementReqVO reqVO) {
        return success(financeReportService.getAssetStatement(reqVO));
    }

    @Operation(summary = "现金流量表查询（按期间月查；期间为空按当前月份）")
    @GetMapping("/cashflow-statement/get")
    @PreAuthorize("@ss.hasAnyPermissions('finance:report:cashflow-statement:list,finance:report:income-statement:list')")
    public CommonResult<FinanceCashflowStatementRespVO> getCashflowStatement(
            @Valid FinanceIncomeStatementReqVO reqVO) {
        return success(financeReportService.getCashflowStatement(reqVO));
    }
}
