package com.kyx.service.finance.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 管理后台 - 经营看板指标 Request VO
 */
@Data
@Schema(description = "管理后台 - 经营看板指标 Request VO")
public class FinanceDashboardMetricsReqVO {

    @Schema(description = "账套ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套ID不能为空")
    private Long companyId;

    @Schema(description = "查询期间 yyyyMM，不传默认当前月份", example = "202603")
    @Pattern(regexp = "^\\d{6}$", message = "查询期间格式非法，应为 yyyyMM")
    private String period;
}
