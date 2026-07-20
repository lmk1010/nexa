package com.kyx.service.hr.controller.admin.training.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

@Data
public class TrainingPlanSaveReqVO {

    private Long id;

    @NotNull(message = "课程不能为空")
    private Long courseId;

    private String planCode;

    @NotBlank(message = "计划名称不能为空")
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

    private String status;

    private String remark;

    private Set<Long> profileIds;

    private Set<Long> deptIds;

}
