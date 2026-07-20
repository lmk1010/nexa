package com.kyx.service.hr.controller.admin.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 薪酬报表 Response VO")
@Data
public class PayrollReportRespVO {

    private Integer year;

    private String currency = "CNY";

    private Integer batchCount = 0;

    private Integer payslipCount = 0;

    private Integer profileCount = 0;

    private Integer deptCount = 0;

    private Integer pendingCount = 0;

    private Integer confirmedCount = 0;

    private Integer issueCount = 0;

    private Integer resolvedCount = 0;

    private BigDecimal baseSalaryTotal = BigDecimal.ZERO;

    private BigDecimal payableSalaryTotal = BigDecimal.ZERO;

    private BigDecimal attendanceDeductionTotal = BigDecimal.ZERO;

    private BigDecimal overtimePayTotal = BigDecimal.ZERO;

    private BigDecimal bonusTotal = BigDecimal.ZERO;

    private BigDecimal allowanceTotal = BigDecimal.ZERO;

    private BigDecimal deductionTotal = BigDecimal.ZERO;

    private BigDecimal socialInsuranceTotal = BigDecimal.ZERO;

    private BigDecimal housingFundTotal = BigDecimal.ZERO;

    private BigDecimal companySocialSecurityTotal = BigDecimal.ZERO;

    private BigDecimal companyHousingFundTotal = BigDecimal.ZERO;

    private BigDecimal taxTotal = BigDecimal.ZERO;

    private BigDecimal netSalaryTotal = BigDecimal.ZERO;

    private BigDecimal laborCostTotal = BigDecimal.ZERO;

    private BigDecimal payableSalaryPerCapita = BigDecimal.ZERO;

    private BigDecimal netSalaryPerCapita = BigDecimal.ZERO;

    private BigDecimal laborCostPerCapita = BigDecimal.ZERO;

    private List<MonthSummary> months = new ArrayList<>();

    private List<DepartmentSummary> departments = new ArrayList<>();

    private List<SalaryStructure> salaryStructures = new ArrayList<>();

    @Data
    public static class MonthSummary {

        private String payrollMonth;

        private Integer batchCount = 0;

        private Integer payslipCount = 0;

        private Integer profileCount = 0;

        private Integer pendingCount = 0;

        private Integer confirmedCount = 0;

        private Integer issueCount = 0;

        private Integer resolvedCount = 0;

        private BigDecimal baseSalaryTotal = BigDecimal.ZERO;

        private BigDecimal payableSalaryTotal = BigDecimal.ZERO;

        private BigDecimal attendanceDeductionTotal = BigDecimal.ZERO;

        private BigDecimal overtimePayTotal = BigDecimal.ZERO;

        private BigDecimal bonusTotal = BigDecimal.ZERO;

        private BigDecimal allowanceTotal = BigDecimal.ZERO;

        private BigDecimal deductionTotal = BigDecimal.ZERO;

        private BigDecimal socialInsuranceTotal = BigDecimal.ZERO;

        private BigDecimal housingFundTotal = BigDecimal.ZERO;

        private BigDecimal companySocialSecurityTotal = BigDecimal.ZERO;

        private BigDecimal companyHousingFundTotal = BigDecimal.ZERO;

        private BigDecimal taxTotal = BigDecimal.ZERO;

        private BigDecimal netSalaryTotal = BigDecimal.ZERO;

        private BigDecimal laborCostTotal = BigDecimal.ZERO;

        private BigDecimal payableSalaryPerCapita = BigDecimal.ZERO;

        private BigDecimal netSalaryPerCapita = BigDecimal.ZERO;

        private BigDecimal laborCostPerCapita = BigDecimal.ZERO;

    }

    @Data
    public static class DepartmentSummary {

        private Long deptId;

        private String deptName;

        private Integer profileCount = 0;

        private Integer payslipCount = 0;

        private BigDecimal baseSalaryTotal = BigDecimal.ZERO;

        private BigDecimal payableSalaryTotal = BigDecimal.ZERO;

        private BigDecimal attendanceDeductionTotal = BigDecimal.ZERO;

        private BigDecimal socialInsuranceTotal = BigDecimal.ZERO;

        private BigDecimal housingFundTotal = BigDecimal.ZERO;

        private BigDecimal companySocialSecurityTotal = BigDecimal.ZERO;

        private BigDecimal companyHousingFundTotal = BigDecimal.ZERO;

        private BigDecimal taxTotal = BigDecimal.ZERO;

        private BigDecimal netSalaryTotal = BigDecimal.ZERO;

        private BigDecimal laborCostTotal = BigDecimal.ZERO;

        private BigDecimal payableSalaryPerCapita = BigDecimal.ZERO;

        private BigDecimal netSalaryPerCapita = BigDecimal.ZERO;

        private BigDecimal laborCostPerCapita = BigDecimal.ZERO;

    }

    @Data
    public static class SalaryStructure {

        private String code;

        private String name;

        private String category;

        private String basisName;

        private BigDecimal amountTotal = BigDecimal.ZERO;

        private BigDecimal ratio = BigDecimal.ZERO;

    }

}
