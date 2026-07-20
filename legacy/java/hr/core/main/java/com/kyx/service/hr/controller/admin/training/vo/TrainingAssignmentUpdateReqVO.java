package com.kyx.service.hr.controller.admin.training.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class TrainingAssignmentUpdateReqVO {

    @NotNull(message = "任务ID不能为空")
    private Long id;

    private String status;

    private Integer progress;

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

}
