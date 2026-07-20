package com.kyx.service.hr.dal.dataobject.scope;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_dept")
@KeySequence("system_dept_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ScopeDeptDO extends TenantBaseDO {

    public static final Long PARENT_ID_ROOT = 0L;

    @TableId
    private Long id;

    private String name;

    private Long parentId;

    private Integer sort;

    private Integer status;
}
