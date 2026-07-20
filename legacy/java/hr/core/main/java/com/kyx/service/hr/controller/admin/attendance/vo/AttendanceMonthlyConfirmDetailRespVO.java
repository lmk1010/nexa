package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 月度考勤确认详情 Response VO")
@Data
public class AttendanceMonthlyConfirmDetailRespVO {

    private AttendanceMonthlySettlementRespVO settlement;

    private AttendanceMonthlyConfirmRespVO confirm;

    private List<AttendanceDailyResultRespVO> dailyResults;

}
