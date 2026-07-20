package com.kyx.service.hr.controller.admin.attendance.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 打卡记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AttendanceClockRecordPageReqVO extends PageParam {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "打卡类型：IN/OUT")
    private String clockType;

    @Schema(description = "来源类型：MANUAL/DINGTALK")
    private String sourceType;

    @Schema(description = "考勤日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] attendanceDate;

}
