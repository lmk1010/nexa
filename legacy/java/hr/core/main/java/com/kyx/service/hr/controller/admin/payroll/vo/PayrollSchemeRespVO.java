package com.kyx.service.hr.controller.admin.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 薪资方案 Response VO")
@Data
public class PayrollSchemeRespVO {

    private Long id;

    private String schemeCode;

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

    private LocalDateTime createTime;

}
