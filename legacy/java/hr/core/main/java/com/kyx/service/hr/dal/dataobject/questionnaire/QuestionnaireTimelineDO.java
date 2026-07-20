package com.kyx.service.hr.dal.dataobject.questionnaire;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 问卷时间线 DO
 */
@TableName("hr_roster_timeline")
@KeySequence("hr_roster_timeline_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionnaireTimelineDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long rosterId;

    private String bizType;

    private Long bizId;

    private String title;

    private Integer status;

    private LocalDateTime occurredAt;

    private String extraJson;

}
