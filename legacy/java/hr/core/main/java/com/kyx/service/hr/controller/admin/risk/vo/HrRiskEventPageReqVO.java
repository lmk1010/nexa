package com.kyx.service.hr.controller.admin.risk.vo;

import com.kyx.foundation.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
@EqualsAndHashCode(callSuper = true)
public class HrRiskEventPageReqVO extends PageParam {

    private String keyword;

    private String severity;

    private String status;

    private String sourceType;

    private String issueType;

    private Long id;

    private Long profileId;

    private Long ownerUserId;

    private Long deptId;

    private Boolean includeChildren;

    private List<Long> profileIds;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime dueTimeStart;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime dueTimeEnd;

}
