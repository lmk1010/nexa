package com.kyx.service.hr.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 员工绩效方案模板表
 */
@TableName("hr_employee_performance_scheme")
@KeySequence("hr_employee_performance_scheme_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeePerformanceSchemeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String schemeCode;

    private String schemeName;

    private String schemeType;

    private String cycleType;

    private String status;

    private Boolean defaultFlag;

    private String templateJson;

    private String reviewFlowJson;

    private LocalDate effectiveDate;

    private LocalDate expireDate;

    private String remark;
}
