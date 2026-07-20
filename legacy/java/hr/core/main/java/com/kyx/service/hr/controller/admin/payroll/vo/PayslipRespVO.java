package com.kyx.service.hr.controller.admin.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工资条 Response VO")
@Data
public class PayslipRespVO {

    private Long id;

    private Long batchId;

    private String payrollMonth;

    private Long profileId;

    private String profileName;

    private Long userId;

    private String userNickname;

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

    private BigDecimal netSalary;

    private String lineItemJson;

    private String status;

    private LocalDateTime confirmedTime;

    private LocalDateTime issueTime;

    private String issueRemark;

    private LocalDateTime resolvedTime;

    private Long resolvedBy;

    private String resolvedByName;

    private String resolveRemark;

}
