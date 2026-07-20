package com.kyx.service.hr.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Employee custom field value.
 */
@TableName("hr_employee_custom_field_value")
@KeySequence("hr_employee_custom_field_value_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeCustomFieldValueDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long profileId;

    private Long fieldId;

    private String fieldKey;

    private String fieldValue;

    private String valueJson;

}
