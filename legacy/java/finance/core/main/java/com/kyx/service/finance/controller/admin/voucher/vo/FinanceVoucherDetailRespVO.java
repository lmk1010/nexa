package com.kyx.service.finance.controller.admin.voucher.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理后台 - 凭证明细 Response VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 凭证明细 Response VO")
@ExcelIgnoreUnannotated
public class FinanceVoucherDetailRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "凭证ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("凭证ID")
    private Long voucherId;

    @Schema(description = "行号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("行号")
    private Integer lineNo;

    @Schema(description = "科目编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @ExcelProperty("科目编码")
    private String subjectCode;

    @Schema(description = "科目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "库存现金")
    @ExcelProperty("科目名称")
    private String subjectName;

    @Schema(description = "往来对象ID")
    @ExcelProperty("往来对象ID")
    private Long contactId;

    @Schema(description = "账户ID")
    @ExcelProperty("账户ID")
    private Long accountId;

    @Schema(description = "借方金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @ExcelProperty("借方金额")
    private BigDecimal debitAmount;

    @Schema(description = "贷方金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.00")
    @ExcelProperty("贷方金额")
    private BigDecimal creditAmount;

    @Schema(description = "税额", example = "0.00")
    @ExcelProperty("税额")
    private BigDecimal taxAmount;

    @Schema(description = "摘要")
    @ExcelProperty("摘要")
    private String description;
}
