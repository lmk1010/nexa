package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - 考勤异常批量处理 Request VO")
@Data
public class AttendanceExceptionBatchResolveReqVO {

    @Schema(description = "异常ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "异常ID不能为空")
    private List<Long> ids;

    @Schema(description = "处理状态")
    private String exceptionStatus;

    @Schema(description = "处理备注")
    private String handleRemark;

}
