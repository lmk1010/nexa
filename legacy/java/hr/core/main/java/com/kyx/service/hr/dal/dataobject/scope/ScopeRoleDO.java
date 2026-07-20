package com.kyx.service.hr.dal.dataobject.scope;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_role")
@KeySequence("system_role_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ScopeRoleDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String name;

    private Integer sort;

    private Integer status;
}
