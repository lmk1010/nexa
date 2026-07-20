package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 月度考勤确认异议处理 Request VO")
@Data
public class AttendanceMonthlyConfirmResolveReqVO {

    @Schema(description = "确认单ID")
    @NotNull(message = "确认单ID不能为空")
    private Long id;

    @Schema(description = "处理说明")
    @Size(max = 1000, message = "处理说明不能超过1000个字符")
    private String resolveRemark;

}
