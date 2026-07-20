package com.kyx.service.hr.dal.dataobject.exam;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * HR 考试答案 DO
 *
 * @author MK
 */
@TableName("hr_exam_answer")
@KeySequence("hr_exam_answer_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExamAnswerDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long attemptId;

    private Long itemId;

    private String answerText;

    private String answerJson;

    private Integer answerScore;

    private Integer manualScore;

    private String aiComment;

    private Integer gradeStatus;

    private String gradeError;

}
