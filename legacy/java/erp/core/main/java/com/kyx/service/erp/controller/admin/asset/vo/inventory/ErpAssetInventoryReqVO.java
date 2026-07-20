package com.kyx.service.erp.controller.admin.asset.vo.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * ERP 资产盘点请求 VO
 */
@Schema(description = "管理后台 - ERP 资产盘点请求 VO")
@Data
public class ErpAssetInventoryReqVO {

    @Schema(description = "盘点计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "盘点计划编号不能为空")
    private Long planId;

    @Schema(description = "扫描内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASSET49341433746")
    @NotBlank(message = "扫描内容不能为空")
    private String scanContent;

    @Schema(description = "扫描方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "qr_code")
    @NotBlank(message = "扫描方式不能为空")
    private String scanMethod; // qr_code, barcode, manual

    @Schema(description = "实际状态", example = "normal")
    private String actualStatus;

    @Schema(description = "实际位置编号", example = "1")
    private Long actualLocationId;

    @Schema(description = "实际位置名称", example = "总部一楼")
    private String actualLocationName;

    @Schema(description = "实际使用人编号", example = "1")
    private Long actualUserId;

    @Schema(description = "实际使用人姓名", example = "张三")
    private String actualUserName;

    @Schema(description = "备注", example = "设备正常使用")
    private String remark;

    @Schema(description = "照片URL", example = "https://example.com/photo.jpg")
    private String photoUrl;

    @Schema(description = "差异说明", example = "状态不符，实际状态为破损")
    private String diffDescription;

    @Schema(description = "资产现值", example = "5000.00")
    private BigDecimal currentValue;
} 