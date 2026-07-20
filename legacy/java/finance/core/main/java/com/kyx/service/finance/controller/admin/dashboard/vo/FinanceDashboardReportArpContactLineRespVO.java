package com.kyx.service.finance.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 管理后台 - 经营看板管理报表应收应付明细行 Response VO
 *
 * @author xyang
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理后台 - 经营看板管理报表应收应付明细行 Response VO")
public class FinanceDashboardReportArpContactLineRespVO {

    @Schema(description = "往来单位ID", example = "1")
    private Long contactId;

    @Schema(description = "往来单位名称", example = "华东供应商A")
    private String contactName;

    @Schema(description = "金额", example = "12000.00")
    private BigDecimal amount;
}

