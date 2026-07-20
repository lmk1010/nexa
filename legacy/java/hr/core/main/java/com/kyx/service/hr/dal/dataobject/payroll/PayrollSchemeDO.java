package com.kyx.service.hr.dal.dataobject.payroll;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payroll scheme and default salary rule.
 */
@TableName("hr_payroll_scheme")
@KeySequence("hr_payroll_scheme_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollSchemeDO extends TenantBaseDO {

    @TableId
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

}
