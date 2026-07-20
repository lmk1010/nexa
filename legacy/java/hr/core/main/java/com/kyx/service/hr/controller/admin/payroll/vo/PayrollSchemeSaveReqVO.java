package com.kyx.service.hr.controller.admin.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 薪资方案保存 Request VO")
@Data
public class PayrollSchemeSaveReqVO {

    private Long id;

    private String schemeCode;

    @NotBlank(message = "方案名称不能为空")
    private String schemeName;

    private String status;

    private Boolean defaultFlag;

    private String salaryType;

    private String currency;

    private BigDecimal baseSalary;

    private BigDecimal allowance;

    private BigDecimal deduction;

    private String taxRuleJson;

    private String attendanceRuleJson;

    private LocalDate effectiveDate;

    private LocalDate expireDate;

    private String remark;

}
