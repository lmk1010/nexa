package com.kyx.service.hr.controller.admin.employee.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "Admin - Employee performance page Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeePerformancePageReqVO extends PageParam {

    private Long profileId;

    @Schema(description = "绩效方案ID")
    private Long schemeId;

    private String profileName;

    private String profileMobile;

    private String period;

    private String grade;

    private String result;

    private String cycleStatus;

    private String applicationType;

    private String applicationStatus;

    private String approvalStatus;

    private BigDecimal scoreMin;

    private BigDecimal scoreMax;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate evaluatedDateStart;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate evaluatedDateEnd;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime interviewTimeStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime interviewTimeEnd;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextFollowTimeStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextFollowTimeEnd;

    @Schema(hidden = true)
    private Set<Long> profileIds;
}
