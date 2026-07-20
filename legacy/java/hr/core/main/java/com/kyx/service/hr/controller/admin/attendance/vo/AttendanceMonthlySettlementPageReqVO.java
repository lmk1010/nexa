package com.kyx.service.hr.controller.admin.attendance.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 考勤月结分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AttendanceMonthlySettlementPageReqVO extends PageParam {

    @Schema(description = "结算月份 yyyy-MM")
    private String settlementMonth;

    @Schema(description = "状态：GENERATED/LOCKED")
    private String status;

}
