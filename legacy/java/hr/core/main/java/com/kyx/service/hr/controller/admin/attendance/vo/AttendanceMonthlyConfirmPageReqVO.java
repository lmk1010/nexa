package com.kyx.service.hr.controller.admin.attendance.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 月度考勤确认分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AttendanceMonthlyConfirmPageReqVO extends PageParam {

    @Schema(description = "月结ID")
    private Long settlementId;

    @Schema(description = "结算月份 yyyy-MM")
    private String settlementMonth;

    @Schema(description = "确认状态：PENDING/CONFIRMED/ISSUE/RESOLVED")
    private String status;

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "用户ID")
    private Long userId;

}
