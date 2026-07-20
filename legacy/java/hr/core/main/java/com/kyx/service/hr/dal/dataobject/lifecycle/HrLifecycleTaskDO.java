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
 * HR lifecycle task/checklist item.
 */
@TableName("hr_lifecycle_task")
@KeySequence("hr_lifecycle_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrLifecycleTaskDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long eventId;

    private Long profileId;

    private String eventType;

    private String taskType;

    private String taskName;

    private Long assigneeUserId;

    private String assigneeName;

    private LocalDate dueDate;

    private String taskStatus;

    private Boolean requiredFlag;

    private Integer sortOrder;

    private LocalDateTime completedTime;

    private Long completedBy;

    private String remark;

}
