package com.kyx.service.hr.controller.admin.administrative.leave.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 假期余额分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrLeaveBalancePageReqVO extends PageParam {

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "假期类型编码")
    private String leaveTypeCode;

    @Schema(description = "年份")
    private Integer year;

}
