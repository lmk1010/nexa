package com.kyx.service.hr.controller.admin.employee.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 员工培训分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeTrainingPageReqVO extends PageParam {

    private Long profileId;

    private String profileName;

    private String profileMobile;

    private String trainingName;

    private String provider;

    private String result;

    private Long courseId;

    private Long planId;

    private Long assignmentId;

    private Long examId;

    private Long questionnaireId;

    private Boolean retrainDueOnly;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate startDateStart;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate startDateEnd;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate endDateStart;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate endDateEnd;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate certificateExpireDateStart;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate certificateExpireDateEnd;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate retrainDateStart;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate retrainDateEnd;

    @Schema(hidden = true)
    private Set<Long> profileIds;

}
