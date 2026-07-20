package com.kyx.service.erp.controller.admin.asset.vo.barcode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 资产条码生成 Request VO")
@Data
public class ErpAssetBarcodeGenerateReqVO {

    @Schema(description = "资产编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "资产编号列表不能为空")
    private List<Long> assetIds;

    @Schema(description = "条码类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "条码类型不能为空")
    private Integer barcodeType;

    @Schema(description = "条码前缀", example = "BC")
    private String barcodePrefix;

    @Schema(description = "发放序号前缀", example = "SN")
    private String serialPrefix;

    @Schema(description = "备注", example = "批量生成条码")
    private String remark;

} 