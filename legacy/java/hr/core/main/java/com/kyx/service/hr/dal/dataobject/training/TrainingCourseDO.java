package com.kyx.service.hr.dal.dataobject.training;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@TableName("hr_training_course")
@KeySequence("hr_training_course_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingCourseDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String courseName;

    private String courseType;

    private String category;

    private String lecturer;

    private String provider;

    private BigDecimal durationHours;

    private String coverUrl;

    private String contentUrl;

    private String materialName;

    private String materialUrl;

    private Long examId;

    private Long questionnaireId;

    private Integer retrainCycleMonths;

    private Integer defaultReminderDays;

    private String description;

    /**
     * 0 enabled, 1 disabled.
     */
    private Integer status;

}
