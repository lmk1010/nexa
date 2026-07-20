package com.kyx.service.hr.dal.dataobject.selfservice;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("hr_employee_quick_action_config")
@KeySequence("hr_employee_quick_action_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrEmployeeQuickActionConfigDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String actionCode;

    private String actionName;

    private String icon;

    private String routePath;

    private String category;

    private String permissionCode;

    private Integer sortOrder;

    private Integer status;

}
