package com.kyx.service.hr.dal.dataobject.lifecycle;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HR lifecycle event.
 */
@TableName("hr_lifecycle_event")
@KeySequence("hr_lifecycle_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrLifecycleEventDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long profileId;

    private Long entryId;

    private Long userId;

    private String employeeName;

    private String eventType;

    private String eventStatus;

    private String sourceType;

    private Long sourceId;

    private String processInstanceId;

    private Long applyUserId;

    private String applyUserName;

    private LocalDateTime applyTime;

    private LocalDate effectiveDate;

    private LocalDateTime completedTime;

    private String beforeJson;

    private String afterJson;

    private String reason;

    private String remark;

}
