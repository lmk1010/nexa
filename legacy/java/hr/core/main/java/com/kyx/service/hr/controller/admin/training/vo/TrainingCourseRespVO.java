package com.kyx.service.hr.controller.admin.training.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TrainingCourseRespVO {

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

    private Integer status;

    private Date createTime;

}
