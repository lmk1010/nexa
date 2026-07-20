package com.kyx.service.hr.controller.admin.payroll.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 工资条分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PayslipPageReqVO extends PageParam {

    @Schema(description = "工资条ID")
    private Long id;

    @Schema(description = "批次ID")
    private Long batchId;

    @Schema(description = "发薪月份 yyyy-MM")
    private String payrollMonth;

    @Schema(description = "状态：DRAFT/PUBLISHED/CONFIRMED/ISSUE/RESOLVED")
    private String status;

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "用户ID")
    private Long userId;

}
