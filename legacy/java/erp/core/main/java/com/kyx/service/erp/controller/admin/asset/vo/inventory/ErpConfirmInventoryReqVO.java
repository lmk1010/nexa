package com.kyx.service.erp.controller.admin.asset.vo.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * ERP 确认盘点请求 VO
 */
@Schema(description = "管理后台 - ERP 确认盘点请求 VO")
@Data
public class ErpConfirmInventoryReqVO {

    @Schema(description = "盘点记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "盘点记录编号不能为空")
    private Long recordId;

    @Schema(description = "差异类型", example = "status_mismatch")
    private String differenceType; // normal, status_mismatch, location_mismatch, information_mismatch

    @Schema(description = "实际状态", example = "damaged")
    private String actualStatus;

    @Schema(description = "实际位置编号", example = "1")
    private Long actualLocationId;

    @Schema(description = "实际位置名称", example = "总部二楼")
    private String actualLocationName;

    @Schema(description = "实际使用人编号", example = "1")
    private Long actualUserId;

    @Schema(description = "实际使用人姓名", example = "李四")
    private String actualUserName;

    @Schema(description = "资产现值", example = "4000.00")
    private BigDecimal currentValue;

    @Schema(description = "照片URL", example = "https://example.com/photo.jpg")
    private String photoUrl;

    @Schema(description = "差异说明", example = "状态不符，实际状态为破损")
    private String diffDescription;

    @Schema(description = "备注", example = "螺丝脱落，无法启动")
    private String remark;
} 