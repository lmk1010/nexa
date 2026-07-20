package com.kyx.service.hr.controller.admin.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 我的个税扣缴汇总 Response VO")
@Data
public class PayrollTaxSummaryRespVO {

    private Integer year;

    private String currency = "CNY";

    private Integer monthCount = 0;

    private Integer payslipCount = 0;

    private Integer pendingCount = 0;

    private Integer confirmedCount = 0;

    private Integer issueCount = 0;

    private Integer resolvedCount = 0;

    private BigDecimal payableSalaryTotal = BigDecimal.ZERO;

    private BigDecimal socialInsuranceTotal = BigDecimal.ZERO;

    private BigDecimal housingFundTotal = BigDecimal.ZERO;

    private BigDecimal withholdingBaseTotal = BigDecimal.ZERO;

    private BigDecimal taxTotal = BigDecimal.ZERO;

    private BigDecimal netSalaryTotal = BigDecimal.ZERO;

    private List<MonthTaxSummary> months = new ArrayList<>();

    @Data
    public static class MonthTaxSummary {

        private String payrollMonth;

        private Integer payslipCount = 0;

        private Integer pendingCount = 0;

        private Integer confirmedCount = 0;

        private Integer issueCount = 0;

        private Integer resolvedCount = 0;

        private BigDecimal payableSalary = BigDecimal.ZERO;

        private BigDecimal socialInsurance = BigDecimal.ZERO;

        private BigDecimal housingFund = BigDecimal.ZERO;

        private BigDecimal withholdingBase = BigDecimal.ZERO;

        private BigDecimal tax = BigDecimal.ZERO;

        private BigDecimal netSalary = BigDecimal.ZERO;

    }

}
