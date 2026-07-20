package com.kyx.service.hr.dal.dataobject.questionnaire;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * HR 问卷选项 DO
 *
 * @author MK
 */
@TableName("hr_questionnaire_option")
@KeySequence("hr_questionnaire_option_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionnaireOptionDO extends TenantBaseDO {

    /**
     * 选项ID
     */
    @TableId
    private Long id;

    /**
     * 题目ID
     */
    private Long itemId;

    /**
     * 选项序号
     */
    private Integer sortNo;

    /**
     * 选项内容
     */
    private String optionText;

    /**
     * 选项分值
     */
    private Integer optionScore;

}
