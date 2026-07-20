package com.kyx.service.hr.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Field-level employee profile change log.
 */
@TableName("hr_employee_change_log")
@KeySequence("hr_employee_change_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeChangeLogDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long profileId;

    private String module;

    private String fieldKey;

    private String fieldName;

    private String beforeValue;

    private String afterValue;

    private String sourceType;

    private Long sourceId;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime operationTime;

}
