package com.kyx.service.erp.controller.admin.asset.vo.barcode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 资产条码打印 Request VO")
@Data
public class ErpAssetBarcodePrintReqVO {

    @Schema(description = "条码打印记录编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "条码打印记录编号列表不能为空")
    private List<Long> barcodePrintIds;

    @Schema(description = "打印数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "打印数量不能为空")
    private Integer printCount;

    @Schema(description = "打印机名称", example = "HP LaserJet Pro MFP M428fdw")
    private String printerName;

    @Schema(description = "备注", example = "批量打印条码")
    private String remark;

} 