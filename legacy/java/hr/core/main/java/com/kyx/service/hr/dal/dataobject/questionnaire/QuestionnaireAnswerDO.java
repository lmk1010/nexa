package com.kyx.service.hr.dal.dataobject.questionnaire;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * HR 问卷答案 DO
 *
 * @author MK
 */
@TableName("hr_questionnaire_answer")
@KeySequence("hr_questionnaire_answer_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionnaireAnswerDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long questionnaireId;

    private Long publishId;

    private Long assignmentId;

    private Long itemId;

    private String answerText;

    private Integer answerScore;

    private String answerJson;

}
