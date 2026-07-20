package com.kyx.service.biz.dal.dataobject.work;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName(value = "business_work_requirement_rate", autoResultMap = true)
@KeySequence("business_work_requirement_rate_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkRequirementRateDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long requirementId;
    private Integer score;
    private String content;
    private Long raterUserId;
    private String raterName;
    private Long targetUserId;
    private String targetUserName;

}
