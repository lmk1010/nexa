package com.kyx.service.erp.controller.admin.asset.vo.assetreturn;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - ERP 资产归还记录新增/修改 Request VO")
@Data
public class ErpAssetReturnSaveReqVO {

    @Schema(description = "归还记录编号", example = "1")
    private Long id;

    @Schema(description = "领用记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "领用记录编号不能为空")
    private Long checkoutId;

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    @Schema(description = "归还人编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "归还人编号不能为空")
    private Long returnUserId;

    @Schema(description = "归还部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "归还部门编号不能为空")
    private Long returnDeptId;

    @Schema(description = "归还日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    @NotNull(message = "归还日期不能为空")
    private LocalDate returnDate;

    @Schema(description = "归还状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "归还状态不能为空")
    private Integer returnCondition;

    @Schema(description = "归还备注", example = "设备完好")
    private String returnRemark;

    @Schema(description = "备注", example = "测试备注")
    private String remark;

} 