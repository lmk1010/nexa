package com.kyx.service.hr.controller.admin.attendance.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "Admin - Attendance correction page Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AttendanceCorrectionPageReqVO extends PageParam {

    @Schema(description = "User id")
    private Long userId;

    @Schema(description = "Profile id")
    private Long profileId;

    @Schema(description = "Apply type: CORRECTION/FIELD")
    private String applyType;

    @Schema(description = "Clock type: IN/OUT")
    private String clockType;

    @Schema(description = "Status: PENDING/APPROVED/REJECTED/CANCELED")
    private String status;

    @Schema(description = "Attendance date range")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] attendanceDate;

}
