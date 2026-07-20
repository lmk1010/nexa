package com.kyx.service.hr.dal.dataobject.exam;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * HR 考试作答记录 DO
 *
 * @author MK
 */
@TableName("hr_exam_attempt")
@KeySequence("hr_exam_attempt_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExamAttemptDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long examId;

    private Long publishId;

    private Long paperId;

    private Long userId;

    private LocalDateTime startAt;

    private LocalDateTime submitAt;

    /**
     * 状态(0进行中 1已提交)
     */
    private Integer status;

    private Integer totalScore;

    private String gradeMode;

    private Integer gradeStatus;

    private String gradeError;

}
