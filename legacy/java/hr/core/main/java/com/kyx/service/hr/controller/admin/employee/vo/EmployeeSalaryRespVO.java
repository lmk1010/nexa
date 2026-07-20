package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 员工薪酬信息 Response VO")
@Data
public class EmployeeSalaryRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "员工档案ID")
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
