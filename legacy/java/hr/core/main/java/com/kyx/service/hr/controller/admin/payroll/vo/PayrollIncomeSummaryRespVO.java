package com.kyx.service.hr.controller.admin.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 我的年度收入汇总 Response VO")
@Data
public class PayrollIncomeSummaryRespVO {

    private Integer year;

    private String currency = "CNY";

    private Integer monthCount = 0;

    private Integer payslipCount = 0;

    private Integer pendingCount = 0;

    private Integer confirmedCount = 0;

    private Integer issueCount = 0;

    private Integer resolvedCount = 0;

    private BigDecimal grossIncomeTotal = BigDecimal.ZERO;

    private BigDecimal payableSalaryTotal = BigDecimal.ZERO;

    private BigDecimal baseSalaryTotal = BigDecimal.ZERO;

    private BigDecimal attendanceDeductionTotal = BigDecimal.ZERO;

    private BigDecimal overtimePayTotal = BigDecimal.ZERO;

    private BigDecimal bonusTotal = BigDecimal.ZERO;

    private BigDecimal allowanceTotal = BigDecimal.ZERO;

    private BigDecimal deductionTotal = BigDecimal.ZERO;

    private BigDecimal socialInsuranceTotal = BigDecimal.ZERO;

    private BigDecimal housingFundTotal = BigDecimal.ZERO;

    private BigDecimal taxTotal = BigDecimal.ZERO;

    private BigDecimal netSalaryTotal = BigDecimal.ZERO;

    private List<MonthSummary> months = new ArrayList<>();

    @Data
    public static class MonthSummary {

        private String payrollMonth;

        private Integer payslipCount = 0;

        private Integer pendingCount = 0;

        private Integer confirmedCount = 0;

        private Integer issueCount = 0;

        private Integer resolvedCount = 0;

        private BigDecimal grossIncome = BigDecimal.ZERO;

        private BigDecimal payableSalary = BigDecimal.ZERO;

        private BigDecimal baseSalary = BigDecimal.ZERO;

        private BigDecimal attendanceDeduction = BigDecimal.ZERO;

        private BigDecimal overtimePay = BigDecimal.ZERO;

        private BigDecimal bonus = BigDecimal.ZERO;

        private BigDecimal allowance = BigDecimal.ZERO;

        private BigDecimal deduction = BigDecimal.ZERO;

        private BigDecimal socialInsurance = BigDecimal.ZERO;

        private BigDecimal housingFund = BigDecimal.ZERO;

        private BigDecimal tax = BigDecimal.ZERO;

        private BigDecimal netSalary = BigDecimal.ZERO;

    }

}
