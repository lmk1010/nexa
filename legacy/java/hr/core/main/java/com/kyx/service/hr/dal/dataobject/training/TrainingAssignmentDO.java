package com.kyx.service.hr.dal.dataobject.training;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("hr_training_assignment")
@KeySequence("hr_training_assignment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingAssignmentDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long planId;

    private Long courseId;

    private Long profileId;

    private Long userId;

    /**
     * NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELED.
     */
    private String status;

    private Integer progress;

    private LocalDateTime completedTime;

    private String result;

    private String certificateName;

    private String certificateUrl;

    private String materialName;

    private String materialUrl;

    private Long examId;

    private Long questionnaireId;

    private LocalDate retrainDate;

    private LocalDate reminderDate;

    private String remark;

    private Integer evaluationScore;

    private String evaluationFeedback;

    private LocalDateTime evaluatedTime;

}
