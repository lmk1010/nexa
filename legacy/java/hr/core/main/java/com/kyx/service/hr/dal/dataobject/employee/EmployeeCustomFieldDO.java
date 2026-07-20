package com.kyx.service.hr.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Employee roster custom field definition.
 */
@TableName("hr_employee_custom_field")
@KeySequence("hr_employee_custom_field_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeCustomFieldDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String fieldKey;

    private String fieldName;

    private String fieldType;

    private String fieldGroup;

    private String optionsJson;

    private Boolean requiredFlag;

    private Boolean sensitiveFlag;

    private String visibleRoles;

    private Integer sortOrder;

    private Integer status;

}
