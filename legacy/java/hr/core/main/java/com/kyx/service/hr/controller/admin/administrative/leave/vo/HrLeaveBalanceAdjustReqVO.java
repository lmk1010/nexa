package com.kyx.service.hr.controller.admin.administrative.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 假期余额调整 Request VO")
@Data
public class HrLeaveBalanceAdjustReqVO {

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "假期类型编码")
    @NotNull(message = "假期类型不能为空")
    private String leaveTypeCode;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "调整额度，正数增加、负数扣减")
    @NotNull(message = "调整额度不能为空")
    private BigDecimal amount;

    @Schema(description = "备注")
    private String remark;

}
