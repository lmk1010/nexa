package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalTime;

@Schema(description = "管理后台 - 考勤班次规则保存 Request VO")
@Data
public class AttendanceShiftRuleSaveReqVO {

    @Schema(description = "班次ID")
    private Long id;

    @Schema(description = "班次名称")
    @NotBlank(message = "班次名称不能为空")
    private String shiftName;

    @Schema(description = "上班时间")
    @NotNull(message = "上班时间不能为空")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @Schema(description = "下班时间")
    @NotNull(message = "下班时间不能为空")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    @Schema(description = "休息开始时间")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime restStartTime;

    @Schema(description = "休息结束时间")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime restEndTime;

    @Schema(description = "迟到宽限分钟")
    private Integer lateGraceMinutes;

    @Schema(description = "早退宽限分钟")
    private Integer earlyLeaveGraceMinutes;

    @Schema(description = "应出勤小时")
    private BigDecimal workHours;

    @Schema(description = "是否默认班次")
    private Boolean defaultFlag;

    @Schema(description = "状态：0正常 1停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
