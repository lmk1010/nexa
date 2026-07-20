package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 员工薪酬信息新增/修改 Request VO")
@Data
public class EmployeeSalarySaveReqVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工档案ID不能为空")
    private Long profileId;

    @Schema(description = "薪酬类型")
    private String salaryType;

    @Schema(description = "薪酬金额")
    private BigDecimal amount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "备注")
    private String remark;
}
