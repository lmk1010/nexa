package com.kyx.service.finance.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 管理后台 - 利润表查询 Request VO
 */
@Data
@Schema(description = "管理后台 - 利润表查询 Request VO")
public class FinanceIncomeStatementReqVO {

    @Schema(description = "账套ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套ID不能为空")
    private Long companyId;

    @Schema(description = "查询期间 yyyyMM，不传默认按当前年份统计", example = "202602")
    @Pattern(regexp = "^\\d{6}$", message = "查询期间格式非法，应为 yyyyMM")
    private String period;
}
