package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 考勤月结锁定 Request VO")
@Data
public class AttendanceMonthlySettlementLockReqVO {

    @Schema(description = "月结ID")
    @NotNull(message = "月结ID不能为空")
    private Long id;

}
