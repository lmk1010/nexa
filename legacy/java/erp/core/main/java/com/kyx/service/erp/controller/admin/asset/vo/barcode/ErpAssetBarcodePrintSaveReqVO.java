package com.kyx.service.erp.controller.admin.asset.vo.barcode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 资产条码打印新增/修改 Request VO")
@Data
public class ErpAssetBarcodePrintSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    @Schema(description = "资产编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASSET001")
    @NotEmpty(message = "资产编码不能为空")
    private String assetNo;

    @Schema(description = "条码编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BC001001")
    @NotEmpty(message = "条码编号不能为空")
    private String barcodeNo;

    @Schema(description = "发放序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SN202401001")
    @NotEmpty(message = "发放序号不能为空")
    private String printSerialNo;

    @Schema(description = "发放日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 10:00:00")
    @NotNull(message = "发放日期不能为空")
    private LocalDateTime issueDate;

    @Schema(description = "条码类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "条码类型不能为空")
    private Integer barcodeType;

    @Schema(description = "条码内容/数据", example = "ASSET001")
    private String barcodeContent;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "条码打印备注")
    private String remark;

} 