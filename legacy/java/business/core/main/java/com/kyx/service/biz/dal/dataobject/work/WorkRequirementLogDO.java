package com.kyx.service.biz.dal.dataobject.work;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("business_work_requirement_log")
@KeySequence("business_work_requirement_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkRequirementLogDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long requirementId;
    private String actionType;
    private Integer fromStatus;
    private Integer toStatus;
    private String remark;
    private Long operatorUserId;
    private String operatorName;

}
