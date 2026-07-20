package com.kyx.service.hr.controller.admin.training.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TrainingAssignmentRespVO {

    private Long id;

    private Long planId;

    private String planName;

    private Long courseId;

    private String courseName;

    private String courseType;

    private String category;

    private String lecturer;

    private String provider;

    private BigDecimal durationHours;

    private String contentUrl;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long profileId;

    private String profileName;

    private String profileMobile;

    private Long userId;

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
