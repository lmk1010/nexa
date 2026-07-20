package com.kyx.service.hr.dal.dataobject.payroll;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Employee payslip.
 */
@TableName("hr_payslip")
@KeySequence("hr_payslip_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PayslipDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long batchId;

    private String payrollMonth;

    private Long profileId;

    private Long userId;

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

    /**
     * DRAFT / PUBLISHED / CONFIRMED / ISSUE / RESOLVED.
     */
    private String status;

    private LocalDateTime confirmedTime;

    private LocalDateTime issueTime;

    private String issueRemark;

    private LocalDateTime resolvedTime;

    private Long resolvedBy;

    private String resolveRemark;

}
