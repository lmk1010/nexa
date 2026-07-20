package com.kyx.service.hr.dal.dataobject.questionnaire;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

/**
 * HR 问卷模板 DO
 *
 * @author MK
 */
@TableName("hr_questionnaire")
@KeySequence("hr_questionnaire_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionnaireDO extends TenantBaseDO {

    /**
     * 问卷ID
     */
    @TableId
    private Long id;

    /**
     * 问卷编码
     */
    private String code;

    /**
     * 问卷名称
     */
    private String name;

    /**
     * 问卷类型(peer/employee_impression/exam)
     */
    private String type;

    /**
     * 状态(0草稿 1已发布 2已关闭)
     */
    private Integer status;

    /**
     * 适用角色(manager/employee/both)
     */
    private String roleScope;

    /**
     * 被评对象规则(JSON)
     */
    private String targetRuleJson;

    /**
     * 周期开始
     */
    private LocalDate periodStart;

    /**
     * 周期结束
     */
    private LocalDate periodEnd;

}
