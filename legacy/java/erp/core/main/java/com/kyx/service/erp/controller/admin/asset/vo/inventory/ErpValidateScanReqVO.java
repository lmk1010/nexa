package com.kyx.service.erp.controller.admin.asset.vo.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * ERP 验证扫码请求 VO
 */
@Schema(description = "管理后台 - ERP 验证扫码请求 VO")
@Data
public class ErpValidateScanReqVO {

    @Schema(description = "盘点计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "盘点计划编号不能为空")
    private Long planId;

    @Schema(description = "扫描内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASSET49341433746")
    @NotBlank(message = "扫描内容不能为空")
    private String scanContent;

    @Schema(description = "扫描方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "qr_code")
    @NotBlank(message = "扫描方式不能为空")
    private String scanMethod; // qr_code, barcode, manual
} 