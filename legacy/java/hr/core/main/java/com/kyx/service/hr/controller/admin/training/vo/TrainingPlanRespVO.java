package com.kyx.service.hr.controller.admin.training.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
public class TrainingPlanRespVO {

    private Long id;

    private Long courseId;

    private String courseName;

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

    private String status;

    private String remark;

    private Integer assignmentCount;

    private Integer completedCount;

    private Integer inProgressCount;

    private Integer notStartedCount;

    private Integer overdueCount;

    private BigDecimal completionRate;

    private Integer evaluationCount;

    private BigDecimal averageEvaluationScore;

    private BigDecimal satisfactionRate;

    private Date createTime;

}
