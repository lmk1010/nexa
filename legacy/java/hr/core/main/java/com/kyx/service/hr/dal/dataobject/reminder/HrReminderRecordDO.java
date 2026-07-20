package com.kyx.service.hr.dal.dataobject.reminder;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * HR reminder record snapshot.
 */
@TableName("hr_reminder_record")
@KeySequence("hr_reminder_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrReminderRecordDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String recordKey;

    private Long ruleId;

    private String ruleCode;

    private String ruleName;

    private String businessType;

    private Long businessId;

    private Long receiverUserId;

    private Long profileId;

    private String title;

    private String content;

    private String severity;

    private String status;

    private String routePath;

    private String sourceType;

    private Long sourceId;

    private LocalDateTime triggerTime;

    private LocalDateTime readTime;

    private String remark;

}
