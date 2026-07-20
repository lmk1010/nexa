package com.kyx.service.hr.controller.admin.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 工资条异议 Request VO")
@Data
public class PayslipIssueReqVO {

    @Schema(description = "工资条ID")
    @NotNull(message = "工资条ID不能为空")
    private Long id;

    @Schema(description = "异议说明")
    @NotBlank(message = "异议说明不能为空")
    @Size(max = 1000, message = "异议说明不能超过1000个字符")
    private String issueRemark;

}
