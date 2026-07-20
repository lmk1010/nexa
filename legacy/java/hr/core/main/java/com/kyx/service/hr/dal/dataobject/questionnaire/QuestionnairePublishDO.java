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
 * HR 问卷发布配置 DO
 *
 * @author MK
 */
@TableName("hr_questionnaire_publish")
@KeySequence("hr_questionnaire_publish_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionnairePublishDO extends TenantBaseDO {

    /**
     * 发布ID
     */
    @TableId
    private Long id;

    /**
     * 问卷ID
     */
    private Long questionnaireId;

    /**
     * 发布范围(JSON)
     */
    private String publishScopeJson;

    /**
     * 发送方式(0立即 1定时)
     */
    private Integer sendType;

    /**
     * 周期类型: 0一次性 1每天 2每周 3每月
     */
    private Integer scheduleType;

    /**
     * 每周几(1-7, 1=周一)
     */
    private Integer scheduleDayOfWeek;

    /**
     * 每月几号(1-31)
     */
    private Integer scheduleDayOfMonth;

    /**
     * 发送时刻 HH:mm
     */
    private String scheduleTime;

    /**
     * 截止时长(小时), 每期发布后多少小时截止
     */
    private Integer deadlineHours;

    /**
     * 上次发布时间
     */
    private LocalDateTime lastPublishTime;

    /**
     * 下次发布时间
     */
    private LocalDateTime nextPublishTime;

    /**
     * 已生成批次数
     */
    private Integer generatedCount;

    /**
     * 当前批次号
     */
    private Integer currentBatchNo;

    /**
     * 当前批次标签
     */
    private String currentBatchLabel;

    /**
     * 当前批次开始时间
     */
    private LocalDateTime currentBatchStartAt;

    /**
     * 当前批次截止时间
     */
    private LocalDateTime currentBatchEndAt;

    /**
     * 定时发送时间(一次性)
     */
    private LocalDateTime sendAt;

    /**
     * 截止时间
     */
    private LocalDateTime deadlineAt;

    /**
     * 提醒规则(JSON)
     */
    private String remindRuleJson;

    /**
     * 上次提醒时间
     */
    private LocalDateTime lastRemindTime;

    /**
     * 状态(0未发布 1已发布 2已截止)
     */
    private Integer status;

}
