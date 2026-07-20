package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 员工积分增加 Request VO")
@Data
public class EmployeePointsAddReqVO {

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工档案ID不能为空")
    private Long profileId;

    @Schema(description = "积分数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "积分数量不能为空")
    @Min(value = 1, message = "积分数量必须大于0")
    private Integer points;

    @Schema(description = "积分原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "积分原因不能为空")
    private String reason;

    @Schema(description = "来源类型：attendance/performance/activity/manual")
    private String sourceType;

    @Schema(description = "来源ID")
    private Long sourceId;

    @Schema(description = "备注")
    private String remark;

}
