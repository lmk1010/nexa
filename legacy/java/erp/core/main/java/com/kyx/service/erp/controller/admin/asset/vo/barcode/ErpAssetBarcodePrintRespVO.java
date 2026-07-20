package com.kyx.service.erp.controller.admin.asset.vo.barcode;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 资产条码打印 Response VO")
@Data
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class ErpAssetBarcodePrintRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long assetId;

    @Schema(description = "资产编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASSET001")
    @ExcelProperty("资产编码")
    private String assetNo;

    @Schema(description = "资产名称", example = "联想电脑")
    @ExcelProperty("资产名称")
    private String assetName;

    @Schema(description = "条码编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BC001001")
    @ExcelProperty("条码编号")
    private String barcodeNo;

    @Schema(description = "发放序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SN202401001")
    @ExcelProperty("发放序号")
    private String printSerialNo;

    @Schema(description = "发放日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 10:00:00")
    @ExcelProperty("发放日期")
    private LocalDateTime issueDate;

    @Schema(description = "条码类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("条码类型")
    private Integer barcodeType;

    @Schema(description = "条码类型名称", example = "一维码")
    @ExcelProperty("条码类型名称")
    private String barcodeTypeName;

    @Schema(description = "条码内容/数据", example = "ASSET001")
    private String barcodeContent;

    @Schema(description = "打印次数", example = "5")
    @ExcelProperty("打印次数")
    private Integer printCount;

    @Schema(description = "最后打印时间", example = "2024-01-01 15:30:00")
    @ExcelProperty("最后打印时间")
    private LocalDateTime lastPrintTime;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "状态名称", example = "正常")
    @ExcelProperty("状态名称")
    private String statusName;

    @Schema(description = "备注", example = "条码打印备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

} 