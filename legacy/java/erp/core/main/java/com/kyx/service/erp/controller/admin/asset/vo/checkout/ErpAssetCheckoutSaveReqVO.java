package com.kyx.service.erp.controller.admin.asset.vo.checkout;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - ERP 资产领用记录新增/修改 Request VO")
@Data
public class ErpAssetCheckoutSaveReqVO {

    @Schema(description = "领用记录编号", example = "1")
    private Long id;

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    @Schema(description = "领用人编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "领用人编号不能为空")
    private Long checkoutUserId;

    @Schema(description = "领用部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "领用部门编号不能为空")
    private Long checkoutDeptId;

    @Schema(description = "领用日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    @NotNull(message = "领用日期不能为空")
    private LocalDate checkoutDate;

    @Schema(description = "预计归还日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expectedReturnDate;

    @Schema(description = "领用原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "办公使用")
    @NotNull(message = "领用原因不能为空")
    private String checkoutReason;

    @Schema(description = "备注", example = "测试备注")
    private String remark;

    @Schema(description = "BPM - 发起人自选审批人")
    private Map<String, List<Long>> startUserSelectAssignees;

} 