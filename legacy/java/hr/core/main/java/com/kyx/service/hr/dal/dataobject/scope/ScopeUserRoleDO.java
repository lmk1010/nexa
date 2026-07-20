package com.kyx.service.hr.dal.dataobject.scope;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_user_role")
@KeySequence("system_user_role_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ScopeUserRoleDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long userId;

    private Long roleId;
}
