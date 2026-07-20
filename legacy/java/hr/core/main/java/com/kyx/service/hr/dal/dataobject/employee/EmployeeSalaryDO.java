package com.kyx.service.hr.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工薪酬信息表
 *
 * @author MK
 */
@TableName("hr_employee_salary")
@KeySequence("hr_employee_salary_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeSalaryDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 员工档案ID
     */
    private Long profileId;

    /**
     * 薪酬类型
     */
    private String salaryType;

    /**
     * 薪酬金额
     */
    private BigDecimal amount;

    /**
     * 币种
     */
    private String currency;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 备注
     */
    private String remark;
}
