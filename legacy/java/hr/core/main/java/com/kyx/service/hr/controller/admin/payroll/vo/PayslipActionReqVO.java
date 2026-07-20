package com.kyx.service.hr.controller.admin.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 工资条操作 Request VO")
@Data
public class PayslipActionReqVO {

    @Schema(description = "工资条ID")
    @NotNull(message = "工资条ID不能为空")
    private Long id;

}
