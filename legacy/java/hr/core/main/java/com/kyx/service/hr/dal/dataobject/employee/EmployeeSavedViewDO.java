package com.kyx.service.hr.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Saved roster view for an HR user.
 */
@TableName("hr_employee_saved_view")
@KeySequence("hr_employee_saved_view_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeSavedViewDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long userId;

    private String viewName;

    private String filterJson;

    private String columnsJson;

    private String sortJson;

    private Boolean defaultView;

}
