package com.kyx.service.erp.controller.admin.asset.vo.barcode;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 资产条码打印分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAssetBarcodePrintPageReqVO extends PageParam {

    @Schema(description = "资产编码", example = "ASSET001")
    private String assetNo;

    @Schema(description = "条码编号", example = "BC001001")
    private String barcodeNo;

    @Schema(description = "发放序号", example = "SN202401001")
    private String printSerialNo;

    @Schema(description = "条码类型", example = "1")
    private Integer barcodeType;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "发放日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] issueDate;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 