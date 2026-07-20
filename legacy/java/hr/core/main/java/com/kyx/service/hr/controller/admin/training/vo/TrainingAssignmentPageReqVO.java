package com.kyx.service.hr.controller.admin.training.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingAssignmentPageReqVO extends PageParam {

    private Long planId;

    private Long courseId;

    private Long profileId;

    private String profileName;

    private String profileMobile;

    private String status;

    private Long examId;

    private Long questionnaireId;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate retrainDateStart;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate retrainDateEnd;

    private Boolean mine;

    @Schema(hidden = true)
    private Set<Long> profileIds;

}
