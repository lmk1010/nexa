package com.kyx.service.hr.controller.admin.attendance.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 考勤异常分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AttendanceExceptionPageReqVO extends PageParam {

    @Schema(description = "异常ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "异常类型")
    private String exceptionType;

    @Schema(description = "异常状态")
    private String exceptionStatus;

    @Schema(description = "处理人ID")
    private Long handlerId;

    @Schema(description = "关键字")
    private String keyword;

    @Schema(description = "考勤日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] attendanceDate;

    @Schema(description = "处理时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] handledTime;

}
