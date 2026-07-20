package com.kyx.service.biz.dal.dataobject.work;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("business_work_requirement_developer")
@KeySequence("business_work_requirement_developer_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkRequirementDeveloperDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long requirementId;
    private Long userId;
    private String userName;
    private Long userTenantId;
    private String memberRole;

}
