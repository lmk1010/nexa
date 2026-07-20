package com.kyx.service.hr.dal.dataobject.training;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@TableName("hr_training_plan")
@KeySequence("hr_training_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingPlanDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long courseId;

    private String planCode;

    private String planName;

    private LocalDate startDate;

    private LocalDate endDate;

    private String targetType;

    private String targetSummary;

    private Long ownerUserId;

    private Long examId;

    private Long questionnaireId;

    private Integer retrainCycleMonths;

    private Integer reminderDays;

    private Boolean requireEvaluation;

    /**
     * DRAFT, PUBLISHED, CLOSED.
     */
    private String status;

    private String remark;

}
