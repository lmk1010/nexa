package com.kyx.service.hr.controller.admin.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 工资批次操作 Request VO")
@Data
public class PayrollBatchActionReqVO {

    @Schema(description = "批次ID")
    @NotNull(message = "批次ID不能为空")
    private Long id;

}
