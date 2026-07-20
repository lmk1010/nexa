package com.kyx.service.hr.controller.admin.training.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class TrainingCourseSaveReqVO {

    private Long id;

    @NotBlank(message = "课程名称不能为空")
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

}
