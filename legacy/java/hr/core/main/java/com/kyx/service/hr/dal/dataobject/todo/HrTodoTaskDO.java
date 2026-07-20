package com.kyx.service.hr.dal.dataobject.todo;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Unified HR todo task.
 */
@TableName("hr_todo_task")
@KeySequence("hr_todo_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrTodoTaskDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long assigneeUserId;

    private Long profileId;

    private String businessType;

    private Long businessId;

    private String taskType;

    private String title;

    private String content;

    private String routePath;

    private String status;

    private String priority;

    private LocalDateTime dueTime;

    private Boolean generatedFlag;

    private LocalDateTime completedTime;

    private Long completedBy;

    private String cancelReason;

}
