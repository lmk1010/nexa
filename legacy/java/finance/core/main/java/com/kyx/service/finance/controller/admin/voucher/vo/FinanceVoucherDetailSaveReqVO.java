package com.kyx.service.finance.controller.admin.voucher.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 管理后台 - 凭证明细新增/修改 Request VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 凭证明细新增/修改 Request VO")
public class FinanceVoucherDetailSaveReqVO {

    @Schema(description = "科目编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotBlank(message = "科目编码不能为空")
    private String subjectCode;

    @Schema(description = "科目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "库存现金")
    @NotBlank(message = "科目名称不能为空")
    private String subjectName;

    @Schema(description = "往来对象ID", example = "1")
    private Long contactId;

    @Schema(description = "账户ID", example = "1")
    private Long accountId;

    @Schema(description = "借方金额", example = "1000.00")
    @DecimalMin(value = "0.00", message = "借方金额不能小于 0")
    private BigDecimal debitAmount;

    @Schema(description = "贷方金额", example = "0.00")
    @DecimalMin(value = "0.00", message = "贷方金额不能小于 0")
    private BigDecimal creditAmount;

    @Schema(description = "税额", example = "0.00")
    @DecimalMin(value = "0.00", message = "税额不能小于 0")
    private BigDecimal taxAmount;

    @Schema(description = "摘要")
    private String description;
}
