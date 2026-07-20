package com.kyx.service.hr.dal.dataobject.scope;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_users")
@KeySequence("system_users_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ScopeUserDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String username;

    private String nickname;

    private Long deptId;

    private Integer status;
}
