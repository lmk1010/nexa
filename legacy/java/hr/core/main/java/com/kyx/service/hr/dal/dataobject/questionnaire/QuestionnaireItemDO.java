package com.kyx.service.hr.dal.dataobject.questionnaire;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * HR 问卷题目 DO
 *
 * @author MK
 */
@TableName("hr_questionnaire_item")
@KeySequence("hr_questionnaire_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionnaireItemDO extends TenantBaseDO {

    /**
     * 题目ID
     */
    @TableId
    private Long id;

    /**
     * 问卷ID
     */
    private Long questionnaireId;

    /**
     * 题目序号
     */
    private Integer sortNo;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 题型(single/multi/score/text/score_text/blank)
     */
    private String itemType;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 最大分
     */
    private Integer maxScore;

}
