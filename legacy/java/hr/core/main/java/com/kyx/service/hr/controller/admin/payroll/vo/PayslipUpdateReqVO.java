package com.kyx.service.hr.controller.admin.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 工资条更新 Request VO")
@Data
public class PayslipUpdateReqVO {

    @Schema(description = "工资条ID")
    @NotNull(message = "工资条ID不能为空")
    private Long id;

    private String currency;

    private BigDecimal baseSalary;

    private BigDecimal attendanceDeduction;

    private BigDecimal overtimePay;

    private BigDecimal bonus;

    private BigDecimal allowance;

    private BigDecimal deduction;

    private BigDecimal socialInsurance;

    private BigDecimal housingFund;

    private BigDecimal tax;

    private String lineItemJson;

}
