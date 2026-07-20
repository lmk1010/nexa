package com.kyx.service.hr.controller.admin.lifecycle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - HR 生命周期任务完成 Request VO")
@Data
public class HrLifecycleTaskCompleteReqVO {

    @Schema(description = "任务 ID", required = true)
    @NotNull(message = "任务 ID 不能为空")
    private Long id;

    @Schema(description = "备注")
    private String remark;

}
