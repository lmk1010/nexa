package com.kyx.service.erp.controller.admin.asset.vo.checkout;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - ERP 资产归还 Request VO")
@Data
public class ErpAssetReturnReqVO {

    @Schema(description = "领用记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "领用记录编号不能为空")
    private Long checkoutId;

    @Schema(description = "实际归还日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    @NotNull(message = "实际归还日期不能为空")
    private LocalDate actualReturnDate;

    @Schema(description = "归还状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "归还状态不能为空")
    private Integer returnCondition;

    @Schema(description = "归还备注", example = "设备完好")
    private String returnRemark;

} 