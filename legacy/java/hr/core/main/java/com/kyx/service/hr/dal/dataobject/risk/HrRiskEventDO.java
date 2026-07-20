package com.kyx.service.hr.dal.dataobject.risk;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("hr_risk_event")
@KeySequence("hr_risk_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrRiskEventDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String sourceType;

    private String sourceKey;

    private String issueType;

    private String severity;

    private String title;

    private String description;

    private String action;

    private Long profileId;

    private Long ownerUserId;

    private String routePath;

    private LocalDateTime dueTime;

    private String status;

    private Boolean generatedFlag;

    private LocalDateTime firstSeenTime;

    private LocalDateTime lastSeenTime;

    private LocalDateTime handledTime;

    private Long handledBy;

    private String handleResult;

    private String remark;

}
