package com.kyx.service.hr.dal.dataobject.reminder;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * HR reminder rule.
 */
@TableName("hr_reminder_rule")
@KeySequence("hr_reminder_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrReminderRuleDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String ruleCode;

    private String ruleName;

    private String businessType;

    private String triggerType;

    private String triggerConfigJson;

    private String receiverConfigJson;

    private String channelConfigJson;

    private Long templateId;

    private Boolean enabled;

    private String remark;

}
