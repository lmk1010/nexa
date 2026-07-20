package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 月度考勤确认操作 Request VO")
@Data
public class AttendanceMonthlyConfirmActionReqVO {

    @Schema(description = "确认单ID")
    @NotNull(message = "确认单ID不能为空")
    private Long id;

}
